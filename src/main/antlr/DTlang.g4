grammar DTlang;

WHITESPACE: [ \t\r\n]+ -> skip;

TYPE: 'int' | 'float';
IF: 'if';
ELSE: 'else';
RETURN: 'return';
YIELD: 'yield';
PRINT: 'print';
NEXT: 'next';
CREATE_COROUTINE: 'create_coroutine';
DEF: 'def';
MAIN: 'main';
COROUTINE: 'coroutine';

COMMA: ',';
POINT: '.';

OP_ASSIGN: '=';
OP_PLUS: '+';
OP_MINUS: '-';
OP_MULTIPLY: '*';
OP_DIVIDE: '/';
OP_POWER: '^';
OP_PERCENT: '%';
OP_EQUAL: '==';
OP_NEQUAL: '!=';
OP_LESS: '<';
OP_MORE: '>';
OP_LESS_EQUAL: '<=';
OP_MORE_EQUAL: '>=';

LBRACKET: '(';
RBRACKET: ')';
LBRACKET_CURLY: '{';
RBRACKET_CURLY: '}';

DIGITS: [0-9]+;
NAME: [a-zA-Z_][a-zA-Z_0-9]*;

program: functionDeclaration* (function | globalVariable)* functionMain;

globalVariable: defVariable;

function: functionCoroutine | functionDef;
functionDeclaration: functionCoroutineDeclaration | functionDefDeclaration;

functionMain: DEF MAIN LBRACKET RBRACKET bracketsStatement;

functionDef: DEF NAME functionArguments bracketsStatement;
functionDefDeclaration: DEF NAME functionArguments;

functionCoroutine:
    DEF COROUTINE NAME functionCoroutineArguments bracketsStatement;
functionCoroutineDeclaration: DEF COROUTINE NAME functionCoroutineArguments;

functionCall:
    NAME LBRACKET (functionCallArguments (COMMA functionCallArguments)*)? RBRACKET;
functionCallArguments: variable | number;

functionReturn: RETURN expression;
functionYield: YIELD expression;
functionArguments: LBRACKET (variable (COMMA variable)*)? RBRACKET;
functionCoroutineArguments: LBRACKET (variable (COMMA variable)*)? RBRACKET;

statement: (expression | functionYield | ifStatement | bracketsStatement)*;
bracketsStatement: LBRACKET_CURLY statement functionReturn? RBRACKET_CURLY;

ifStatement:
    IF LBRACKET expression RBRACKET bracketsStatement elseifStatement* elseStatement?;
elseifStatement: (ELSE ifStatement);
elseStatement: (ELSE bracketsStatement);

expression:
    (print | next | createCoroutine)                  # exprExistFunctionCall
    | functionCall                                    # exprFunctionCall
    | number                                          # exprNumber
    | variable                                        # exprVariable
    | defVariable                                     # exprDefVariable
    | LBRACKET expression RBRACKET                    # exprBrackets
    | expression OP_PERCENT                           # exprPercentage
    | (OP_MINUS | OP_PLUS) expression                 # exprUnaryOperators
    | expression OP_POWER expression                  # exprPower
    | expression (OP_MULTIPLY | OP_DIVIDE) expression # exprMultiplyDivide
    | expression (OP_PLUS | OP_MINUS) expression      # exprPlusMinus
    | expression comparison expression                # exprComparison
    | convertVariable                                 # exprConversion;

print: PRINT LBRACKET expression RBRACKET;
next: NEXT LBRACKET variable RBRACKET;
createCoroutine: CREATE_COROUTINE LBRACKET functionCall RBRACKET;

comparison:
    OP_EQUAL
    | OP_NEQUAL
    | OP_LESS
    | OP_MORE
    | OP_LESS_EQUAL
    | OP_MORE_EQUAL;

bracket: LBRACKET | RBRACKET;
bracketCurly: LBRACKET_CURLY | RBRACKET_CURLY;

numInteger: OP_MINUS? DIGITS;
numFloat: numInteger POINT DIGITS;
number: numInteger | numFloat;

defVariable: variable OP_ASSIGN expression;
convertVariable: LBRACKET TYPE RBRACKET (variable | number);
variable: NAME;
