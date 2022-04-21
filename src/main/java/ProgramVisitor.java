import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public class ProgramVisitor extends DTlangBaseVisitor<String> {
    private List<List<String>> variables;
    private List<String> functions;
    private int functionArgNum;
    private List<Integer> functionArgNums;

    @Override
    public String visitProgram(DTlangParser.ProgramContext ctx) {
        variables = new ArrayList<>();
        variables.add(new ArrayList<>());
        functions = new ArrayList<>();
        functionArgNums = new ArrayList<>();
        return "#include <cmath>\n\n" + visitDefault(ctx) + "\n";
    }

    @Override
    public String visitMainFunction(DTlangParser.MainFunctionContext ctx) {
        return visitDefault(ctx).replaceFirst("def", "int ") + "\n";
    }

    @Override
    public String visitFunction(DTlangParser.FunctionContext ctx) {
        var funcName = ctx.getChild(1).getText();
        if (!isVariablesContain(funcName)) {
            functions.add(funcName);
        } else {
            throw new IllegalArgumentException(prettifyErrorMessage(ctx, "\'" + funcName + "\' redefinition"));
        }
        var res = visitFunctionHelper(ctx) + "\n";
        functionArgNums.add(functionArgNum);
        return res;
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
        return visitDefault(ctx) + "\n";
    }

    @Override
    public String visitFunctionDeclaration(DTlangParser.FunctionDeclarationContext ctx) {
        return visitFunctionHelper(ctx) + ";\n\n";
    }

    @Override
    public String visitFunctionArguments(DTlangParser.FunctionArgumentsContext ctx) {
        variables.add(new ArrayList<>());
        var builder = new StringBuilder();
        for (var child : ctx.children) {
            String str;
            if (child instanceof DTlangParser.VariableContext) {
                var varName = visit(child);
                str = "T" + functionArgNum + " " + varName;
                functionArgNum++;
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
        return "return " + visit(ctx.getChild(1)) + ";";
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
                if (!str.endsWith("\n")) {
                    str += ";\n";
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
        res = "{\n" + res.substring(1, res.length() - 1) + "\n}\n";
        variables.remove(variables.size() - 1);
        return res;
    }

    @Override
    public String visitIfStatement(DTlangParser.IfStatementContext ctx) {
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
    public String visitDefVariable(DTlangParser.DefVariableContext ctx) {
        var res = visitDefault(ctx) + ";\n";
        var varName = visit(ctx.getChild(0));
        if (isVariablesContain(varName)) {
            return res;
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
        functionArgNum = 0;
        String res = visitDefault(ctx).replaceFirst("def", "auto ");
        if (functionArgNum != 0) {
            String template = "template <";
            for (int i = 0; i < functionArgNum; i++) {
                template += "class T" + i + ", ";
            }
            res = template.substring(0, template.length() - 2) + ">\n" + res;
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
