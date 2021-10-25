package ru.teosa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Parser {

	private final File ymlConfigFile;

	private List<String> ymlConfigLines;

	private final List<ConfigurationLine> configurationLineList = new ArrayList<>();

	private static final String HELM_LINE_TEMPLATE = "--set %s=%s /";

	private static final Pattern KEY_ONLY_PATTERN = Pattern.compile("^.*:$");

	private static final Pattern KEY_PATTERN = Pattern.compile("^([^:]+)");

	private static final Pattern VALUE_PATTERN = Pattern.compile("(?<=[a-zA-Z0-9]:).*$");

	private static final Pattern SPACES_PATTERN = Pattern.compile("^\\s*");

	public Parser(File ymlConfigFile) {
		this.ymlConfigFile = ymlConfigFile;
	}

	public void parse() {
		readAllLines();
		transform();
		writeHelmConfig();
	}

	private void readAllLines() {
		try {
			ymlConfigLines = Files.readAllLines(Paths.get(ymlConfigFile.getAbsolutePath()));
		} catch (IOException e) {
			throw new RuntimeException("Read file error.", e);
		}
	}

	private void transform() {

		List<KeyLineInfo> keys = new ArrayList<>();

		for (int i = 0; i < ymlConfigLines.size(); i++) {
			if(ymlConfigLines.get(i).trim().length() == 0) {
				continue;
			}

			ConfigurationLine configurationLine = operateInnerLines(i, keys);
			configurationLineList.add(configurationLine);

			// Если мы проваливались по ключам, то двигаем индекс на ту строку, из которой в итоге достали значение.
			// Если не проваливались, то тут будет присуммирован 0
			if (configurationLine.getLineValueIndex() > i) {
				i = configurationLine.getLineValueIndex();
			}

			keys = configurationLine.getMultiKeyList();
		}
	}

	private ConfigurationLine operateInnerLines(int lineIndex, List<KeyLineInfo> previousKeys) {
		String currentLine = ymlConfigLines.get(lineIndex);
		int currentLineSpaces = countSpaces(currentLine);

		previousKeys = previousKeys
				.stream()
				.filter(key -> key.getSpaces() < currentLineSpaces)
				.collect(Collectors.toList());

		if(checkIsLineOnlyKey(currentLine)) {
			previousKeys.add(new KeyLineInfo(currentLineSpaces, extractKey(currentLine)));
			return operateInnerLines(lineIndex + 1, previousKeys);
		} else {
			return operateValueLine(lineIndex, previousKeys);
		}
	}

	private int countSpaces(String line) {
		Matcher matcher = SPACES_PATTERN.matcher(line);
		if (matcher.find()) {
			return matcher.group(0).length();
		} else {
			return 0;
		}
	}


	private ConfigurationLine operateValueLine(int lineIndex, List<KeyLineInfo> previousKeys) {
		String lineWithValue = ymlConfigLines.get(lineIndex);
		String helmKey = "";

		// Собираем ключ из предыдущих значений и текущего
		if (!previousKeys.isEmpty()) {
			for (KeyLineInfo key : previousKeys) {
				helmKey = appendString(helmKey, key.getValue(), ".");
			}
		}

		helmKey = appendString(helmKey, extractKey(lineWithValue), ".");

		previousKeys.add(new KeyLineInfo(countSpaces(lineWithValue), extractKey(lineWithValue)));

		ConfigurationLine configurationLine = new ConfigurationLine();
		configurationLine.setLineValueIndex(lineIndex);
		configurationLine.setMultiKeyList(previousKeys);
		configurationLine.setHelmKey(helmKey);
		// Извлекаем значение
		configurationLine.setHelmValue(extractValue(lineWithValue));

		return configurationLine;
	}

	// Достаем ключ из строки.
	// Принцип тот же что и в checkIsLineOnlyKey(), плюс отпиливаем двоеточие
	private String extractKey(String line) {
		Matcher matcher = KEY_PATTERN.matcher(line);
		if (matcher.find()) {
			String key = matcher.group(0);
			key = key.trim();

			// Убираем -
			if (key.startsWith("-")) {
				key = key.substring(1).trim();
			}

			return key;
		} else {
			return "EMPTY KEY FROM LINE " + line;
		}
	}

	private String extractValue(String line) {
		Matcher matcher = VALUE_PATTERN.matcher(line);
		if (matcher.find()) {
			String value = matcher.group(0);

			// Убираем лишние пробелы
			value = value.trim();

			// Убираем кавычки
			if (value.startsWith("\"") || value.startsWith("\'")) {
				if (value.length() > 2) {
					value = value.substring(1, value.length() - 1);
				}
			}

			return value;
		} else {
			return "EMPTY VALUE FROM LINE " + line;
		}
	}

	// Проверяем что строка содержит только ключ.
	// Для этого она должна содержать набор символов и двоеточие, которое является концом строки
	private boolean checkIsLineOnlyKey(String line) {
		Matcher matcher = KEY_ONLY_PATTERN.matcher(line.trim());
		return matcher.find();
	}

	private void writeHelmConfig() {

		for (ConfigurationLine line : configurationLineList) {
			System.out.println(
					String.format(
							HELM_LINE_TEMPLATE,
							line.getHelmKey(),
							line.getHelmValue()
					)
			);
		}
	}

	// Склейка строк с указанием разделителя
	private String appendString(String first, String second, String separator) {
		separator = separator == null ? ", " : separator;
		if (first == null) {
			return second;
		}
		if (first.isEmpty()) {
			return second;
		}
		if (second != null && !second.isEmpty()) {
			return first + separator + second;
		} else {
			return first;
		}
	}

}
