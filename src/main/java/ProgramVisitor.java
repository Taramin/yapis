import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public class ProgramVisitor extends DTlangBaseVisitor<String> {
    private List<List<String>> variables;
    private List<String> functions;
    private int functionArgCount;
    private List<Integer> functionArgNums;
    private int yieldCount;
    private boolean isCoroutine;

    @Override
    public String visitProgram(DTlangParser.ProgramContext ctx) {
        variables = new ArrayList<>();
        variables.add(new ArrayList<>());
        functions = new ArrayList<>();
        functionArgNums = new ArrayList<>();
        return "#include <cmath>\n#include <iostream>\n" + visitDefault(ctx);
    }

    @Override
    public String visitFunction(DTlangParser.FunctionContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitFunctionCallArguments(DTlangParser.FunctionCallArgumentsContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitGlobalVariable(DTlangParser.GlobalVariableContext ctx) {
        var varName = visit(ctx.getChild(0).getChild(0));
        if (isVariablesContain(varName)) {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, "\'" + varName + "\' redefinition"));
        }
        return visitDefault(ctx);
    }

    @Override
    public String visitFunctionDeclaration(DTlangParser.FunctionDeclarationContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitFunctionDef(DTlangParser.FunctionDefContext ctx) {
        var funcName = ctx.getChild(1).getText();
        if (!isVariablesContain(funcName)) {
            functions.add(funcName);
        } else {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, "\'" + funcName + "\' redefinition"));
        }

        isCoroutine = false;
        var res = visitFunctionHelper(ctx);
        functionArgNums.add(functionArgCount);
        return res;
    }

    @Override
    public String visitFunctionDefDeclaration(DTlangParser.FunctionDefDeclarationContext ctx) {
        return visitFunctionHelper(ctx) + ";";
    }

    @Override
    public String visitFunctionCoroutine(DTlangParser.FunctionCoroutineContext ctx) {
        var funcName = ctx.getChild(2).getText();
        if (!isVariablesContain(funcName)) {
            functions.add(funcName);
        } else {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, "\'" + funcName + "\' redefinition"));
        }

        functionArgCount = 0;
        yieldCount = 0;
        isCoroutine = true;
        String res = visitDefault(ctx).replaceFirst("defcoroutine", "class ");

        String template = "template <";
        String defVariables = "int state = 0;";
        String constructorDecl = funcName + "(";
        String constructorVars = "";
        for (int i = 0; i < functionArgCount; i++) {
            var varName = variables.get(variables.size() - 1).get(i);
            constructorDecl += "T" + i + " " + varName + ",";
            constructorVars += varName + "(" + varName + ")" + ",";
            defVariables += "T" + i + " " + varName + ";";
            template += "class T" + i + ", ";
        }

        String states = "switch(state){" + "case 0:" + "goto start;";
        for (int i = 0; i < yieldCount - 1; i++) {
            states += "case " + (i + 1) + ":" + "goto state" + i + ";";
        }
        states += "default:throw std::runtime_error(\"Coroutine is over\");";
        states += "}" + "start:;";

        if (functionArgCount != 0) {
            constructorDecl = constructorDecl.substring(0, constructorDecl.length() - 1);
            constructorVars = ":" + constructorVars.substring(0, constructorVars.length() - 1);
        }
        constructorDecl += ")";
        String constructor = constructorDecl + constructorVars + "{}";

        int pos = res.indexOf("{");
        res = res.substring(0, pos) +
                "{" +
                defVariables +
                "public:" +
                constructor +
                "auto next() {" +
                states +
                res.substring(pos + 1) +
                "};";

        if (functionArgCount != 0) {
            res = template.substring(0, template.length() - 2) + ">" + res;
        }

        variables.remove(variables.size() - 1);
        functionArgNums.add(functionArgCount);
        return res;
    }

    @Override
    public String visitFunctionCoroutineArguments(DTlangParser.FunctionCoroutineArgumentsContext ctx) {
        variables.add(new ArrayList<>());
        for (var child : ctx.children) {
            if (child instanceof DTlangParser.VariableContext) {
                var varName = visit(child);
                functionArgCount++;
                if (!isVariablesContain(varName)) {
                    addToVariables(varName);
                } else {
                    throw new IllegalArgumentException(prettifyErrorMessage(ctx, "\'" + varName + "\' redefinition"));
                }
            }
        }
        return "";
    }

    @Override
    public String visitFunctionCoroutineDeclaration(DTlangParser.FunctionCoroutineDeclarationContext ctx) {
        functionArgCount = 0;
        String res = visitDefault(ctx).replaceFirst("defcoroutine", "class ");
        if (functionArgCount != 0) {
            String template = "template <";
            for (int i = 0; i < functionArgCount; i++) {
                template += "class T" + i + ", ";
            }
            res = template.substring(0, template.length() - 2) + ">" + res;
        }
        variables.remove(variables.size() - 1);
        return res += ";";
    }

    @Override
    public String visitFunctionMain(DTlangParser.FunctionMainContext ctx) {
        isCoroutine = false;
        return visitDefault(ctx).replaceFirst("def", "int ");
    }

    @Override
    public String visitFunctionArguments(DTlangParser.FunctionArgumentsContext ctx) {
        variables.add(new ArrayList<>());
        var builder = new StringBuilder();
        for (var child : ctx.children) {
            String str;
            if (child instanceof DTlangParser.VariableContext) {
                var varName = visit(child);
                str = "T" + functionArgCount + " " + varName;
                functionArgCount++;
                if (!isVariablesContain(varName)) {
                    addToVariables(varName);
                } else {
                    throw new IllegalArgumentException(prettifyErrorMessage(ctx, "\'" + varName + "\' redefinition"));
                }
            } else if (child instanceof TerminalNodeImpl) {
                str = child.getText();
            } else {
                str = visit(child);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    @Override
    public String visitFunctionCall(DTlangParser.FunctionCallContext ctx) {
        var funcName = ctx.getChild(0).getText();
        if (!functions.contains(funcName)) {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, funcName + "() is undefined"));
        }
        int num = 0;
        var builder = new StringBuilder();
        for (var child : ctx.children) {
            String str;
            if (child instanceof DTlangParser.FunctionCallArgumentsContext) {
                num++;
            }
            if (child instanceof TerminalNodeImpl) {
                str = child.getText();
            } else {
                str = visit(child);
            }
            builder.append(str);
        }
        int funcArgNum = functionArgNums.get(functions.indexOf(funcName));
        if (funcArgNum != num) {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx,
                    funcName + "() must have " + funcArgNum + " arguments, but " + num + " are provided"));
        }
        return builder.toString();
    }

    @Override
    public String visitFunctionReturn(DTlangParser.FunctionReturnContext ctx) {
        if (isCoroutine) {
            throw new IllegalArgumentException(
                    prettifyErrorMessage(ctx, "Unable to use return in the coroutine function"));
        }
        return "return " + visit(ctx.getChild(1)) + ";";
    }

    @Override
    public String visitFunctionYield(DTlangParser.FunctionYieldContext ctx) {
        if (!isCoroutine) {
            throw new IllegalArgumentException(
                    prettifyErrorMessage(ctx, "Unable to use yield in the default function"));
        }
        var res = "state++;";
        res += "return " + visit(ctx.getChild(1)) + ";";
        res += "state" + yieldCount + ":;";
        yieldCount++;
        return res;
    }

    @Override
    public String visitStatement(DTlangParser.StatementContext ctx) {
        if (ctx.children == null) {
            return "";
        }
        var builder = new StringBuilder();
        for (var child : ctx.children) {
            String str;
            if (child instanceof TerminalNodeImpl) {
                str = child.getText();
            } else if (child instanceof DTlangParser.ExpressionContext) {
                str = visit(child);
                if (!str.endsWith(";")) {
                    str += ";";
                }
            } else {
                str = visit(child);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    @Override
    public String visitBracketsStatement(DTlangParser.BracketsStatementContext ctx) {
        variables.add(new ArrayList<String>());
        var res = visitDefault(ctx);
        res = "{" + res.substring(1, res.length() - 1) + "}";
        variables.remove(variables.size() - 1);
        return res;
    }

    @Override
    public String visitIfStatement(DTlangParser.IfStatementContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprExistFunctionCall(DTlangParser.ExprExistFunctionCallContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitElseStatement(DTlangParser.ElseStatementContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitElseifStatement(DTlangParser.ElseifStatementContext ctx) {
        return "else " + visit(ctx.getChild(1));
    }

    @Override
    public String visitExprBrackets(DTlangParser.ExprBracketsContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprComparison(DTlangParser.ExprComparisonContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprConversion(DTlangParser.ExprConversionContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprMultiplyDivide(DTlangParser.ExprMultiplyDivideContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprPercentage(DTlangParser.ExprPercentageContext ctx) {
        var res = visitDefault(ctx);
        res = res.replace("%", "*0.01");
        return "(" + res + ")";
    }

    @Override
    public String visitExprPlusMinus(DTlangParser.ExprPlusMinusContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprPower(DTlangParser.ExprPowerContext ctx) {
        return "std::pow(" + visit(ctx.getChild(0)) + "," + visit(ctx.getChild(2)) + ")";
    }

    @Override
    public String visitExprFunctionCall(DTlangParser.ExprFunctionCallContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprUnaryOperators(DTlangParser.ExprUnaryOperatorsContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprDefVariable(DTlangParser.ExprDefVariableContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprNumber(DTlangParser.ExprNumberContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitExprVariable(DTlangParser.ExprVariableContext ctx) {
        var varName = visitDefault(ctx);
        if (!isVariablesContain(varName)) {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, varName + " is undefined"));
        }
        return varName;
    }

    @Override
    public String visitPrint(DTlangParser.PrintContext ctx) {
        return visitDefault(ctx).replaceFirst("print", "std::cout<<") + "<<\"\\n\"";
    }

    @Override
    public String visitNext(DTlangParser.NextContext ctx) {
        var res = visitDefault(ctx).replaceFirst("next", "");
        return res += ".next()";
    }

    @Override
    public String visitCreateCoroutine(DTlangParser.CreateCoroutineContext ctx) {
        return visitDefault(ctx).replaceFirst("create_coroutine", "");
    }

    @Override
    public String visitDefVariable(DTlangParser.DefVariableContext ctx) {
        var res = visitDefault(ctx) + ";";
        var varName = visit(ctx.getChild(0));
        if (isVariablesContain(varName)) {
            return res;
        }
        if (isCoroutine) {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, "Unable to define variable in coroutine"));
        }
        addToVariables(varName);
        return "auto " + res;
    }

    @Override
    public String visitVariable(DTlangParser.VariableContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitConvertVariable(DTlangParser.ConvertVariableContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitBracket(DTlangParser.BracketContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitBracketCurly(DTlangParser.BracketCurlyContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitComparison(DTlangParser.ComparisonContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitNumFloat(DTlangParser.NumFloatContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitNumInteger(DTlangParser.NumIntegerContext ctx) {
        return visitDefault(ctx);
    }

    @Override
    public String visitNumber(DTlangParser.NumberContext ctx) {
        return visitDefault(ctx);
    }

    private String visitDefault(ParserRuleContext ctx) {
        if (ctx.children == null) {
            return "";
        }
        var builder = new StringBuilder();
        for (var child : ctx.children) {
            String str;
            if (child instanceof TerminalNodeImpl) {
                str = child.getText();
            } else {
                str = visit(child);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    private String visitFunctionHelper(ParserRuleContext ctx) {
        functionArgCount = 0;
        String res = visitDefault(ctx).replaceFirst("def", "auto ");
        if (functionArgCount != 0) {
            String template = "template <";
            for (int i = 0; i < functionArgCount; i++) {
                template += "class T" + i + ", ";
            }
            res = template.substring(0, template.length() - 2) + ">" + res;
        }
        variables.remove(variables.size() - 1);
        return res;
    }

    private boolean isVariablesContain(String variable) {
        for (var list : variables) {
            if (list.contains(variable)) {
                return true;
            }
        }
        return false;
    }

    private void addToVariables(String variable) {
        variables.get(variables.size() - 1).add(variable);
    }

    private String prettifyErrorMessage(ParserRuleContext ctx, String msg) {
        var start = ctx.getStart();
        return "Error: " + msg + " [Ln " + start.getLine() + ", Col " + start.getCharPositionInLine() + "]";
    }
}
