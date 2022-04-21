grammar DTlang;

WHITESPACE: [ \t\r\n]+ -> skip;

TYPE: 'int' | 'float';
IF: 'if';
ELSE: 'else';
RETURN: 'return';
DEF: 'def';
MAIN: 'main';

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

COMMA: ',';
POINT: '.';

LBRACKET: '(';
RBRACKET: ')';
LBRACKET_CURLY: '{';
RBRACKET_CURLY: '}';

DIGITS: [0-9]+;
NAME: [a-zA-Z_][a-zA-Z_0-9]*;

program:
	functionDeclaration* (function | globalVariable)* mainFunction;

globalVariable: defVariable;

mainFunction: DEF MAIN LBRACKET RBRACKET bracketsStatement;
function: DEF NAME functionArguments bracketsStatement;
functionDeclaration: DEF NAME functionArguments;

functionCall:
	NAME LBRACKET (
		functionCallArguments (COMMA functionCallArguments)*
	)? RBRACKET;
functionCallArguments: variable | number;

functionReturn: RETURN expression;
functionArguments:
	LBRACKET (variable (COMMA variable)*)? RBRACKET;

statement: (expression | ifStatement | bracketsStatement)*;
bracketsStatement:
	LBRACKET_CURLY statement functionReturn? RBRACKET_CURLY;

ifStatement:
	IF LBRACKET expression RBRACKET bracketsStatement elseifStatement* elseStatement?;
elseifStatement: (ELSE ifStatement);
elseStatement: (ELSE bracketsStatement);

expression:
	functionCall										# exprFunctionCall
	| number											# exprNumber
	| variable											# exprVariable
	| defVariable										# exprDefVariable
	| LBRACKET expression RBRACKET						# exprBrackets
	| expression OP_PERCENT								# exprPercentage
	| (OP_MINUS | OP_PLUS) expression					# exprUnaryOperators
	| expression OP_POWER expression					# exprPower
	| expression (OP_MULTIPLY | OP_DIVIDE) expression	# exprMultiplyDivide
	| expression (OP_PLUS | OP_MINUS) expression		# exprPlusMinus
	| expression comparison expression					# exprComparison
	| convertVariable									# exprConversion;

variable: NAME;
defVariable: variable OP_ASSIGN expression;
convertVariable: LBRACKET TYPE RBRACKET (variable | number);

numInteger: OP_MINUS? DIGITS;
numFloat: numInteger POINT DIGITS;
number: numInteger | numFloat;

bracket: LBRACKET | RBRACKET;
bracketCurly: LBRACKET_CURLY | RBRACKET_CURLY;

comparison:
	OP_EQUAL
	| OP_NEQUAL
	| OP_LESS
	| OP_MORE
	| OP_LESS_EQUAL
	| OP_MORE_EQUAL;
