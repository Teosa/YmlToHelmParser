package ru.teosa;

import java.util.List;
import lombok.Data;

@Data
public class ConfigurationLine {

	private String helmKey;

	private String helmValue;

	private int lineValueIndex;

	private List<KeyLineInfo> multiKeyList;

//	private int spaceQty;

}
