package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import com.ibm.wala.types.TypeReference;

/**
 * Represents a data dependence between two PDGNodes
 * 
 * @author nchen
 * 
 */
public class DataDependence {
	PDGNode source;

	PDGNode dest;

	String variableName; //TODO: Change this to something more specific unlike String

	TypeReference variableType;

	public DataDependence(PDGNode source, PDGNode dest, TypeReference variableType, String variableName) {
		this.source= source;
		this.dest= dest;
		this.variableName= variableName;
		this.variableType= variableType;
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + ((dest == null) ? 0 : dest.hashCode());
		result= prime * result + ((source == null) ? 0 : source.hashCode());
		result= prime * result + ((variableName == null) ? 0 : variableName.hashCode());
		result= prime * result + ((variableType == null) ? 0 : variableType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataDependence other= (DataDependence)obj;
		if (dest == null) {
			if (other.dest != null)
				return false;
		} else if (!dest.equals(other.dest))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		if (variableType == null) {
			if (other.variableType != null)
				return false;
		} else if (!hasEqualVariableType(other))
			return false;
		return true;
	}

	/**
	 * We redefine how we are comparing our variableTypes because the default equals in
	 * TypeReference compares only by pointer equality and we want to compare the references of its
	 * fields.
	 * 
	 * Why does pointer comparison of fields (i.e., using ==) work? Because there is only one
	 * singleton reference for primitive types.
	 * 
	 * @param other
	 * @return
	 */
	private boolean hasEqualVariableType(DataDependence other) {
		return variableType.getClassLoader() == other.variableType.getClassLoader() && variableType.getName() == other.variableType.getName();
	}

	public String getSimplifiedRepresentation() {
		return variableType.toString() + " " + variableName;
	}

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append(source.getSimplifiedRepresentation());
		sb.append(" -- ");
		sb.append(variableType.toString() + " " + variableName);
		sb.append(" -->");
		sb.append(dest.getSimplifiedRepresentation());
		return sb.toString();
	}
}

// Does nothing but is just a placeholder to represent an object that empty dependence in the PDG.
class NullDataDepedence extends DataDependence {

	private NullDataDepedence(PDGNode source, PDGNode dest, TypeReference variableType, String variableName) {
		super(source, dest, variableType, variableName);
	}

	public static NullDataDepedence make() {
		return new NullDataDepedence(null, null, null, "");
	}

}
