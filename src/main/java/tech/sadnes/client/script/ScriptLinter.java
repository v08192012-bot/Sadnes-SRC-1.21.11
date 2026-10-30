package tech.sadnes.client.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tech.sadnes.client.script.editor.ScriptApiDocs;
import tech.sadnes.libs.rhino.CompilerEnvirons;
import tech.sadnes.libs.rhino.Parser;
import tech.sadnes.libs.rhino.ast.AstNode;
import tech.sadnes.libs.rhino.ast.AstRoot;
import tech.sadnes.libs.rhino.ast.FunctionCall;
import tech.sadnes.libs.rhino.ast.FunctionNode;
import tech.sadnes.libs.rhino.ast.Name;
import tech.sadnes.libs.rhino.ast.ObjectProperty;
import tech.sadnes.libs.rhino.ast.PropertyGet;
import tech.sadnes.libs.rhino.ast.Scope;
import tech.sadnes.libs.rhino.ast.StringLiteral;

public final class ScriptLinter {
    private static final Set<String> BUILTIN_GLOBALS = Set.of(
        "Math",
        "JSON",
        "Date",
        "String",
        "Number",
        "Boolean",
        "Object",
        "Array",
        "RegExp",
        "Error",
        "TypeError",
        "parseInt",
        "parseFloat",
        "isNaN",
        "isFinite",
        "undefined",
        "NaN",
        "Infinity",
        "arguments",
        "this",
        "encodeURIComponent",
        "decodeURIComponent"
    );

    private ScriptLinter() {
    }

    public static List<ScriptLinter.Diagnostic> lint(String string, String string2, Set<String> strings) {
        ArrayList arrayList = new ArrayList();

        AstRoot astRoot;
        try {
            CompilerEnvirons compilerEnvirons = new CompilerEnvirons();
            compilerEnvirons.setLanguageVersion(200);
            compilerEnvirons.setIdeMode(true);
            compilerEnvirons.setRecordingComments(false);
            astRoot = new Parser(compilerEnvirons).parse(string, string2, 1);
        } catch (Exception exception) {
            return arrayList;
        }

        astRoot.visitAll(astNode -> {
            if (astNode instanceof PropertyGet) {
                checkPropertyAccess((PropertyGet)astNode, string, arrayList);
                return true;
            }

            if (astNode instanceof Name) {
                checkName((Name)astNode, strings, arrayList);
            }

            return true;
        });
        return arrayList;
    }

    private static void checkPropertyAccess(PropertyGet propertyGet, String string, List<ScriptLinter.Diagnostic> diagnostics) {
        if (propertyGet.getTarget() instanceof Name name && propertyGet.getProperty() != null) {
            String string2 = name.getIdentifier();
            String string3 = propertyGet.getProperty().getIdentifier();
            Scope scope = name.getDefiningScope();
            String string4;
            if (scope == null) {
                string4 = ScriptApiDocs.getGlobalType(string2);
            } else if (scope instanceof FunctionNode functionNode && isParameter(functionNode, string2)) {
                string4 = resolveCallbackType(functionNode);
            } else {
                string4 = ScriptApiDocs.inferType(string2, string);
            }

            if (!string4.isEmpty() && ScriptApiDocs.hasOwner(string4)) {
                if (!ScriptApiDocs.hasMember(string4, string3)) {
                    diagnostics.add(new ScriptLinter.Diagnostic(propertyGet.getProperty().getLineno(), "у " + string2 + " нет " + string3 + "()"));
                }
            }
        }
    }

    private static boolean isParameter(FunctionNode functionNode, String string) {
        for (AstNode astNode : functionNode.getParams()) {
            if (astNode instanceof Name name && string.equals(name.getIdentifier())) {
                return true;
            }
        }

        return false;
    }

    private static String resolveCallbackType(FunctionNode functionNode) {
        if (!(functionNode.getParent() instanceof FunctionCall functionCall && functionCall.getTarget() instanceof PropertyGet propertyGet && propertyGet.getTarget() instanceof Name name)) {
            return "";
        } else {
            if (!"events".equals(name.getIdentifier())) {
                return "";
            }

            String string = propertyGet.getProperty().getIdentifier();
            if (!string.equals("on") && !string.equals("once")) {
                return ScriptApiDocs.getEventOwner(string);
            }

            List list = functionCall.getArguments();
            if (!list.isEmpty()) {
                Object object = list.getFirst();
                if (object instanceof StringLiteral) {
                    return ScriptApiDocs.getEventOwner(((StringLiteral)object).getValue());
                }
            }

            return "";
        }
    }

    private static void checkName(Name name, Set<String> strings, List<ScriptLinter.Diagnostic> diagnostics) {
        if (name.getDefiningScope() == null) {
            String string = name.getIdentifier();
            if (string != null && !string.isEmpty()) {
                if (!isPropertyName(name)) {
                    if (!BUILTIN_GLOBALS.contains(string) && !strings.contains(string)) {
                        if (!ScriptApiDocs.isGlobal(string)) {
                            diagnostics.add(new ScriptLinter.Diagnostic(name.getLineno(), "неизвестное имя " + string));
                        }
                    }
                }
            }
        }
    }

    private static boolean isPropertyName(Name name) {
        AstNode astNode = name.getParent();
        if (astNode instanceof PropertyGet) {
            return ((PropertyGet)astNode).getProperty() == name;
        } else {
            return astNode instanceof ObjectProperty ? ((ObjectProperty)astNode).getLeft() == name : false;
        }
    }

    public record Diagnostic(int line, String message) {
    }
}
