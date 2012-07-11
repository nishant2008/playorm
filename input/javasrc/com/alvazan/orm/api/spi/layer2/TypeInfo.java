package com.alvazan.orm.api.spi.layer2;

/**
 * NOTE: The type information is either from another constant or from the column's type so we have to 
 * compare against one or the other in this bean.
 * 
 * @author dhiller
 *
 */
public class TypeInfo {

	private TypeEnum constantType;
	/**
	 * NOTE: The type information is either from another constant or from the column's type so we have to 
	 * compare against one or the other field in this bean.
	 */	
	private MetaColumnDbo columnInfo;

	public TypeInfo(TypeEnum type) {
		this.constantType = type;
	}

	public TypeInfo(MetaColumnDbo columnInfo) {
		this.columnInfo = columnInfo;
	}

	public TypeEnum getConstantType() {
		return constantType;
	}

	public void setConstantType(TypeEnum constantType) {
		this.constantType = constantType;
	}

	public MetaColumnDbo getColumnInfo() {
		return columnInfo;
	}

	public void setColumnInfo(MetaColumnDbo columnName) {
		this.columnInfo = columnName;
	}
	
	
}