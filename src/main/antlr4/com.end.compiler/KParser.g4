parser grammar KParser;

//? 0 1
//* 0-..
//+ 1-..

//TODO: сhar

options {tokenVocab=KLexer; }


number: RBO* (INTEGER|DOUBLE) RBC*;
boolean_var:  RBO* (KEYWORD_true|KEYWORD_false) RBC*;
ident: RBO* NAME RBO* ;
concrete_var: number| boolean_var;
variable: concrete_var|ident;

//TODO: ПРИОРИРЕТ СКОБОК
multiply: ( number|ident)((MUL|DIV) (number| ident))*;
add: multiply((ADD|SUB) (multiply))*;

arithExpr:add;

compare: arithExpr (GE|LE|NEQUALS|EQUALS|GT|LT)arithExpr;

negation: NOT (boolean_var);

div_op:(INTEGER|ident) DOT KEYWORD_div RBO (INTEGER|ident) RBC;
rem_op:(INTEGER|ident) DOT KEYWORD_rem RBO (INTEGER|ident) RBC;
inc_op:(INTEGER|ident) DOT KEYWORD_inc RBO RBC;
dec_op:(INTEGER|ident) DOT KEYWORD_dec RBO RBC;
print_op: KEYWORD_print RBO expr RBC;
println_op: KEYWORD_println RBO expr RBC;
readLine_op:KEYWORD_readLine RBO RBC NNV;

//могут быть записаны как expr; но тогда не имеют смысла
//                    так и при присвоении
expr:  variable
     | arithExpr
     | compare
     | readLine_op
     | negation
     | div_op
     | rem_op
     | inc_op
     | dec_op
     | arr_type_size_def_val
     | array_access;

type:KEYWORD_int|KEYWORD_double|KEYWORD_boolean|KEYWORD_array '<'type'>';

declaration: (KEYWORD_val|KEYWORD_var) NAME COLON type  (ASSIGN expr)?;
assignment: NAME ASSIGN expr;

arr_type_size_def_val: KEYWORD_array '<'type'>' RBO INTEGER COMMA CBO expr CBC RBC;
array_access: NAME SBO INTEGER SBC;

//полноценные выражения, имеющие смысл
expression:    assignment
             | declaration
             | if_else
             | loop
             | print_op
             | println_op
             | expr;

expressions: (SEMICOLON* expression SEMICOLON*)*;
block:  CBO (  expressions | expression) CBC;

if_else:KEYWORD_if RBO (compare|BOOLEAN) RBC (expression | block) (KEYWORD_else (expression | block ))?;

loop:(KEYWORD_while  RBO (compare|BOOLEAN) RBC (expression | block) )
     |(KEYWORD_for  RBO ( ident KEYWORD_in ident) RBC  (expression | block) )
     |(KEYWORD_do block  KEYWORD_while RBO (compare|BOOLEAN) RBC)
     ;

//TODO:check it
 funParameter: NAME COLON type;
 funParameters: RBO (funParameter (COMMA funParameter)*)? RBC;
 funDeclaration: KEYWORD_fun NAME funParameters(COLON type)? block;
 classBody: CBO (declaration| funDeclaration)* CBC;
 classDeclaration: KEYWORD_class NAME classBody;
 program: (classDeclaration+ | funDeclaration+) ;









