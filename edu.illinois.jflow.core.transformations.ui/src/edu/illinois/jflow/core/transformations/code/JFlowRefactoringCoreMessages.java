package edu.illinois.jflow.core.transformations.code;

import org.eclipse.osgi.util.NLS;

public class JFlowRefactoringCoreMessages extends NLS {
	private static final String BUNDLE_NAME= "edu.illinois.jflow.core.transformations.code.refactoring"; //$NON-NLS-1$

	public static String InvertLoopAnalyzer_does_not_contain_closure;

	public static String InvertLoopAnalyzer_not_on_loop;

	public static String InvertLoopRefactoring_name;

	public static String ExtractClosureRefactoring_bundle_textedit_description;

	public static String ExtractClosureRefactoring_name;

	public static String ExtractClosureRefactoring_change_name;

	public static String ExtractClosureRefactoring_descriptor_description;

	public static String ExtractClosureRefactoring_descriptor_description_short;

	public static String ExtractClosureRefactoring_error_nameInUse;

	public static String ExtractClosureRefactoring_error_sameParameter;

	public static String ExtractClosureRefactoring_organize_imports;

	public static String ExtractClosureRefactoring_replace_continue;

	public static String ExtractClosureRefactoring_replace_occurrences;

	public static String ExtractClosureRefactoring_substitute_with_call;

	public static String ExtractClosureRefactoring_visibility_pattern;

	public static String ExtractClosureAnalyzer_after_do_keyword;

	public static String ExtractClosureAnalyzer_ambiguous_return_value;

	public static String ExtractClosureAnalyzer_assignments_to_local;

	public static String ExtractClosureAnalyzer_branch_break_mismatch;

	public static String ExtractClosureAnalyzer_branch_continue_mismatch;

	public static String ExtractClosureAnalyzer_branch_mismatch;

	public static String ExtractClosureAnalyzer_cannot_determine_return_type;

	public static String ExtractClosureAnalyzer_cannot_extract_anonymous_type;

	public static String ExtractClosureAnalyzer_cannot_extract_for_initializer;

	public static String ExtractClosureAnalyzer_cannot_extract_for_updater;

	public static String ExtractClosureAnalyzer_cannot_extract_from_annotation;

	public static String ExtractClosureAnalyzer_cannot_extract_method_name_reference;

	public static String ExtractClosureAnalyzer_cannot_extract_name_in_declaration;

	public static String ExtractClosureAnalyzer_cannot_extract_null_type;

	public static String ExtractClosureAnalyzer_cannot_extract_part_of_qualified_name;

	public static String ExtractClosureAnalyzer_cannot_extract_switch_case;

	public static String ExtractClosureAnalyzer_cannot_extract_type_reference;

	public static String ExtractClosureAnalyzer_cannot_extract_variable_declaration;

	public static String ExtractClosureAnalyzer_cannot_extract_variable_declaration_fragment;

	public static String ExtractClosureAnalyzer_compile_errors;

	public static String ExtractClosureAnalyzer_compile_errors_no_parent_binding;

	public static String ExtractClosureAnalyzer_leftHandSideOfAssignment;

	public static String ExtractClosureAnalyzer_only_method_body;

	public static String ExtractClosureAnalyzer_parent_mismatch;

	public static String ExtractClosureAnalyzer_resource_in_try_with_resources;

	public static String ExtractClosureAnalyzer_single_expression_or_set;

	public static String ExtractClosureAnalyzer_super_or_this;

	public static String FlowAnalyzer_execution_flow;

	public static String StatementAnalyzer_doesNotCover;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, JFlowRefactoringCoreMessages.class);
	}

	private JFlowRefactoringCoreMessages() {
	}
}
