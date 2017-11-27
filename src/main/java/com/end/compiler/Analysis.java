package com.end.compiler;

import java.util.*;

public class Analysis {

    //---------------------------------NODE analysis---------------------------------------//
    public static void analyze(Program program) {
        Utils.getAllChildren(program, Expr.class).forEach(x -> x.fillType(getType(x)));

        program.getClassDeclarationList().forEach(Analysis::analyze);
        program.getFunDeclarationList().forEach(Analysis::analyze);
    }

    private static void analyze(ClassDeclaration classDeclaration) {
        //class varName dublications
        List<ClassDeclaration> classList = new ArrayList<>();
        classList.addAll(Utils.getAllVisibleNodes(classDeclaration, ClassDeclaration.class));
        if (classList.size() > 0) {
            classList.remove(classDeclaration);
            classList.stream().forEach(x -> {
                if (x.name().equals(classDeclaration.name()))
                    PrintableErrors.printConflict(classDeclaration.position,
                            classDeclaration, x);
            });
        }

        Utils.getAllChildren(classDeclaration, FunDeclaration.class).forEach(Analysis::analyze);
    }

    private static void analyze(FunDeclaration funDeclaration) {
        //fun dubliations
        List<FunDeclaration> funList = new ArrayList<>();
        funList.addAll(Utils.getAllVisibleNodes(funDeclaration, FunDeclaration.class));
        if (funList.size() > 0) {
            funList.remove(funDeclaration);
            funList.stream().forEach(x -> {
                if ((x.getFunName().getVarName().equals(funDeclaration.getFunName().getVarName())) &&
                        (areParamsListsEqual(funDeclaration.getFunParametersList(), x.getFunParametersList())))
                    PrintableErrors.printConflict(funDeclaration.position,
                            funDeclaration, x);
            });
        }

        funDeclaration.getExpressionList().forEach(Analysis::analyze);
//        Utils.getAllChildren(funDeclaration, Expression.class).stream()
//                .forEach(Analysis::analyze);

        //Type: retirnExpr and declared
        if (funDeclaration.getReturnType() != null && funDeclaration.getReturnExpr() != null) {
            Type returnExpressionType = getType(funDeclaration.getReturnExpr());
//            if (returnExpressionType == null && funDeclaration.getReturnType() != null) {
//                analyze(funDeclaration.getReturnExpr());
//                PrintableErrors.printTypeMismatchError(
//                        funDeclaration.getReturnType(),
//                        returnExpressionType,
//                        funDeclaration.getPosition());
//            }
//            else
            if (returnExpressionType != null &&
                    !typesAreEqual(funDeclaration.getReturnType(), getType(funDeclaration.getReturnExpr())))
                PrintableErrors.printTypeMismatchError(
                        funDeclaration.getReturnType(),
                        returnExpressionType,
                        funDeclaration.getReturnExpr().getPosition());
        } else if (funDeclaration.getReturnType() != null && funDeclaration.getReturnExpr() == null)
            PrintableErrors.printNoReturnStatement(funDeclaration.position);
        else if (funDeclaration.getReturnType() == null && funDeclaration.getReturnExpr() != null)
            PrintableErrors.printTypeMismatchError(new Unit(), getType(funDeclaration.getReturnExpr()),
                    funDeclaration.getReturnExpr().getPosition());
    }
    //-------------------------------------------------------------------------------------------------------------//

    //------------------------EXPRESSION analysis-----------------------------------------------------------------//
    private static void analyze(Expression expression) {
        if (expression.getClass().getSimpleName().equals(Assignment.class.getSimpleName()))
            analyze((Assignment) expression);
        else if (expression.getClass().getSimpleName().equals(WhileLoop.class.getSimpleName()))
            analyze((WhileLoop) expression);
        else if (expression.getClass().getSimpleName().equals(ForLoop.class.getSimpleName()))
            analyze((ForLoop) expression);
        else if (expression.getClass().getSimpleName().equals(DoWhileLoop.class.getSimpleName()))
            analyze((DoWhileLoop) expression);
        else if (expression.getClass().getSimpleName().equals(Declaration.class.getSimpleName()))
            analyze((Declaration) expression);
        else if (expression.getClass().getSimpleName().equals(IfElse.class.getSimpleName()))
            analyze((IfElse) expression);
        else if (expression.getClass().getSimpleName().equals(ElseBlock.class.getSimpleName()))
            analyze((ElseBlock) expression);
    }

    private static void analyze(Declaration declaration) {
        //variable was declared
        List<Declaration> declList = new ArrayList<>();
        declList.addAll(Utils.getAllVisibleNodes(declaration, Declaration.class));
        declList.remove(declaration);
        declList.stream().forEach(x -> {
            if (x.getVariable().getVarName().equals(declaration.getVariable().getVarName()))
                PrintableErrors.printConflict(declaration.position,
                        declaration, x);
        });

        analyze(declaration.getExpr());

        Type foundType = getType(declaration.getExpr());
        if (!typesAreEqual(foundType, declaration.getType()))
            PrintableErrors.printTypeMismatchError(
                    declaration.getType(),
                    foundType,
                    declaration.position);
    }

    private static void analyze(Assignment assignment) {
        analyze(assignment.getValue());

        Optional<Declaration> declaration = Utils.getAllVisibleNodes(assignment, Declaration.class).stream()
                .filter(x -> ((VariableReference) assignment.getLeft()).getVarName().equals(x.getVariable().getVarName())).findFirst();
        if (!declaration.isPresent())
            PrintableErrors.printUnresolvedReferenceError(assignment.getLeft().name(), assignment.position);
        else { //if variable was declared check types
            Type expectedType = declaration.get().getType();
            Type actualType = getType(assignment.getValue());

            //именно eqals type, not castable
            // нельзя var wrong: Double=9;
            if (!typesAreEqual(expectedType, actualType))
                PrintableErrors.printTypeMismatchError(
                        expectedType,
                        actualType,
                        assignment.position);
        }
    }

    private static void analyze(WhileLoop whileLoop) {
        analyze(whileLoop.getCondition());

        Type actualType = getType(whileLoop.getCondition());
        if (!typesAreEqual(actualType,new Boolean()))
            PrintableErrors.printTypeMismatchError
                    (new Boolean(), actualType, whileLoop.getCondition().position);
        whileLoop.getExpressions().forEach(Analysis::analyze);
    }

    private static void analyze(DoWhileLoop doWhileLoop) {
        analyze(doWhileLoop.getCondition());

        Type actualType = getType(doWhileLoop.getCondition());
        if (!typesAreEqual(actualType,new Boolean()))
            PrintableErrors.printTypeMismatchError
                    (new Boolean(), actualType, doWhileLoop.getCondition().position);
        doWhileLoop.getExpressions().forEach(Analysis::analyze);
    }

    private static void analyze(ForLoop forLoop) {
        if (typesAreEqual(getType(forLoop.getIterable()), new Array()))
            PrintableErrors.printIsNotIterableError(forLoop.getIterable().position);

        analyze(forLoop.getIterator());
        analyze(forLoop.getIterable());
        forLoop.getExpressions().forEach(Analysis::analyze);
    }

    private static void analyze(IfElse ifElse) {

        analyze(ifElse.getCondition());

        Type actualType = getType(ifElse.getCondition());
        if (actualType != new Boolean())
            PrintableErrors.printTypeMismatchError(new Boolean(), actualType, ifElse.getCondition().position);

        List<Expression> expressionList = new ArrayList<>();
        expressionList.addAll(Utils.getAllChildren(ifElse, Expression.class));

        //dublictations
        expressionList.stream().filter
                (x -> Collections.frequency(expressionList, x) > 1)
                .forEach(x -> PrintableErrors.printDublicatesError("expression " + x.name(), x.position));
        expressionList.forEach(Analysis::analyze);

        analyze(ifElse.getElseBlock());

    }

    private static void analyze(ElseBlock elseBlock) {

        List<Expression> expressionList = new ArrayList<>();
        expressionList.addAll(Utils.getAllChildren(elseBlock, Expression.class));

        //dublictations
        expressionList.stream().filter
                (x -> Collections.frequency(expressionList, x) > 1)
                .forEach(x -> PrintableErrors.printDublicatesError("expression " + x.name(), x.position));

        expressionList.forEach(Analysis::analyze);


    }
//----------------------------------------------------------------------------------------------------------------//

    //-----------------------------EXPR analysis--------------------------------------------------------------------//
    private static void analyze(Expr expr) {
        if (expr.getClass().getSimpleName().equals(BinaryExpr.class.getSimpleName()))
            analyze((BinaryExpr) expr);
        else if (expr.getClass().getSimpleName().equals(NewVariable.class.getSimpleName()))
            analyze((NewVariable) expr);
        else if (expr.getClass().getSimpleName().equals(VariableReference.class.getSimpleName()))
            analyze((VariableReference) expr);
        else if (expr.getClass().getSimpleName().equals(FunCall.class.getSimpleName()))
            analyze((FunCall) expr);
        else if (expr.getClass().getSimpleName().equals(ArrayAccess.class.getSimpleName()))
            analyze((ArrayAccess) expr);
        else if (expr.getClass().getSimpleName().equals(ArrTypeSizeDefVal.class.getSimpleName()))
            analyze((ArrTypeSizeDefVal) expr);
        //IntegerVar, BooleanVar, CharVar, DoubleVar->nothing to analyze
    }

    private static void analyze(BinaryExpr binaryExpr) {
        analyze(binaryExpr.getLeft());
        analyze(binaryExpr.getRight());
        Type resolvedType = resolveType(
                getType(binaryExpr.getLeft()), getType(binaryExpr.getRight()),
                binaryExpr.getLeft(), binaryExpr.getRight());
        if (resolvedType == null)
            if (getType(binaryExpr.getRight()) != null && getType(binaryExpr.getLeft()) != null)
                PrintableErrors.printIncompatibleTypesError(
                        getType(binaryExpr.getLeft()),
                        getType(binaryExpr.getRight()),
                        binaryExpr.position);
            else analyzeOperation(binaryExpr, resolvedType);
    }

    private static void analyzeOperation(BinaryExpr binaryExpr, Type resolvedType) {

//        switch (binaryExpr.getSign()){
//            case ">=":
//            case "<=":
//            case "<":
//            case ">":
//            case "==":
//            case "!=":
//
        //Char подерживает все операции
        if (typesAreEqual(resolvedType, new Array()) || typesAreEqual(resolvedType, new Boolean()))
            PrintableErrors.printOperationDoesNotSupportError
                    (binaryExpr.getSign(), resolvedType, binaryExpr.position);
    }

    private static void analyze(NewVariable newVariable) { //TODO: check it
        //была ли такая уже
        if (Utils.getAllVisibleNodes(newVariable, NewVariable.class)
                .stream().anyMatch(x ->
                {
                    return (x.name()).equals(newVariable.name());
                }))
            PrintableErrors.printDublicatesError("variable", newVariable.position);
    }

    private static void analyze(FunCall funCall) {
        //была ли вызываемая функция объявлена
        if (Utils.getAllVisibleNodes(funCall, FunDeclaration.class)
                .stream().noneMatch(x ->
                {
                    return (x.getFunName().name().equals(funCall.getName())
                            && (paramsListsAreEqual(x.getFunParametersList(), funCall.getParameters())));
                })) PrintableErrors.printNoSuchFunctionError(funCall, funCall.position);
    }

    private static void analyze(VariableReference variableReference) {
        //была ли переменная объявлена
        if (!((Utils.getAllVisibleNodes(variableReference, NewVariable.class)
                .stream().anyMatch(x -> (x.getVariable().getVarName()).equals(variableReference.getVarName()))
                ||
                Utils.getAllVisibleNodes(variableReference, ForLoop.class)
                        .stream().anyMatch(x ->
                        (x.getIterator().getVarName()).equals(variableReference.getVarName())))
                ||
                Utils.getAllVisibleNodes(variableReference, Declaration.class)
                        .stream().anyMatch(x -> (x.getVariable().getVarName()).equals(variableReference.getVarName()))))

            PrintableErrors.printUnresolvedReferenceError(variableReference.getVarName(), variableReference.position);
    }

    private static boolean paramsListsAreEqual(List<FunParameter> list1, List<Expr> list2) {
        if (list1 == null || list2 == null) return true;
        else {
            if (list1.size() != list2.size()) return false;
            for (int i = 0; i < list1.size(); i++) {
                if (typesAreEqual(list1.get(i).getType(), list2.get(i).getType())) return false;
            }
        }
        return true;
    }

    private static boolean areParamsListsEqual(List<FunParameter> list1, List<FunParameter> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 != null && list2 != null) {
            if (list1.size() != list2.size()) return false;
            for (int i = 0; i < list1.size(); i++) {
                if (!typesAreEqual(list1.get(i).getType(), list2.get(i).getType())) return false;
            }
        } else return false;
        return true;
    }

    private static void analyze(ArrayAccess arrayAccess) {
        analyze(arrayAccess.getExpr());
        Optional<Declaration> declaration = Utils.getAllVisibleNodes(arrayAccess, Declaration.class).stream()
                .filter(x -> arrayAccess.getName().equals(x.name())).findFirst();
        if (!declaration.isPresent())
            PrintableErrors.printUnresolvedReferenceError(arrayAccess.name(), arrayAccess.position);
        else {
            //TODO: or typesAreEqualOrCanCast?
            if (!typesAreEqual(declaration.get().getType(), getType(arrayAccess.getExpr())))
                PrintableErrors.printTypeMismatchError(
                        declaration.get().getType(),
                        getType(arrayAccess.getExpr()),
                        arrayAccess.position);
        }
    }

    private static void analyze(ArrTypeSizeDefVal arrTypeSizeDefVal) {
        Utils.getAllChildren(arrTypeSizeDefVal, Expr.class).forEach(Analysis::analyze);
    }

    private static void analyze(ReturnExpr returnExpr) {
        analyze(returnExpr.getExpr());
    }
    //----------------------------------------------------------------------------------------------------//

    //----------------------TYPE analysis--------------------------------------------------------------------//
    private static Type getType(Expr expr) {
        if (expr.getType() != null) {
            return expr.getType();
        } else {
            expr.fillType(exploreType(expr));
            return expr.getType();
        }
    }

    private static Type exploreType(Expr expr) {
        if (expr.getClass().getSimpleName().equals(BinaryExpr.class.getSimpleName())) {
            AutoCastType(getType(((BinaryExpr) expr).getLeft()), getType(((BinaryExpr) expr).getRight()));
            if (Arrays.asList("!=", "==", ">=", "<=", ">", "<")
                    .contains(((BinaryExpr) expr).getSign()))
                return new Boolean();
            else return resolveType(getType(((BinaryExpr) expr).getLeft()), getType(((BinaryExpr) expr).getRight()),
                    ((BinaryExpr) expr).getLeft(), ((BinaryExpr) expr).getRight());
        } else if (expr.getClass().getSimpleName().equals(NewVariable.class.getSimpleName()))
            return exploreType((NewVariable) expr);
        else if (expr.getClass().getSimpleName().equals(VariableReference.class.getSimpleName()))
            return exploreType((VariableReference) expr);
        else if (expr.getClass().getSimpleName().equals(IntegerVar.class.getSimpleName()))
            return new Integer();
        else if (expr.getClass().getSimpleName().equals(CharVar.class.getSimpleName()))
            return new Char();
        else if (expr.getClass().getSimpleName().equals(DoubleVar.class.getSimpleName()))
            return new Double();
        else if (expr.getClass().getSimpleName().equals(BooleanVar.class.getSimpleName()))
            return new Boolean();
        else if (expr.getClass().getSimpleName().equals(FunCall.class.getSimpleName()))
            return exploreType((FunCall) expr);
        else if (expr.getClass().getSimpleName().equals(ArrayAccess.class.getSimpleName()))
            return exploreType((ArrayAccess) expr);
        else if (expr.getClass().getSimpleName().equals(ArrTypeSizeDefVal.class.getSimpleName()))
            return exploreType((ArrTypeSizeDefVal) expr);
        else if (expr.getClass().getSimpleName().equals(ReturnExpr.class.getSimpleName()))
            return getType(((ReturnExpr) expr).getExpr());
        else throw new UnsupportedOperationException(expr.getClass().getCanonicalName());

    }

    private static Type exploreType(NewVariable newVariable) {
        return newVariable.getType();
    }

    private static Type exploreType(ArrTypeSizeDefVal arrTypeSizeDefVal) {
        return new Array(arrTypeSizeDefVal.getNestedType());
    }

    private static Type exploreType(ArrayAccess arrayAccess) {
        Optional<Declaration> declaration = Utils.getAllVisibleNodes(arrayAccess, Declaration.class).stream()
                .filter(x -> (x.getVariable().getVarName().equals(arrayAccess.getName())
                        && (typesAreEqual(x.getType(), new Array())))).findFirst();
        if (declaration.isPresent()) return declaration.get().getType();//.getNrstedType();
        return null;
        //TODO: write code here!!!!!!!!!!!!!!!!!!!!
    }

    private static Type exploreType(FunCall funCall) {
        Optional<FunDeclaration> funDeclaration =
                Utils.getAllVisibleNodes(funCall, FunDeclaration.class).stream()
                        .filter(x ->
                                (x.getFunName().name().equals(funCall.getName())
                                        && (paramsListsAreEqual(x.getFunParametersList(), funCall.getParameters())))).
                        findFirst();
        if (funDeclaration.isPresent()) return funDeclaration.get().getReturnType();
        return null;
    }

    private static Type resolveType(Type type1, Type type2, Expr expr1, Expr expr2) {
        if (typesAreEqual(type1, type2)) return type1;
        else {
            Type autoCastType = AutoCastType(type1, type2);
            if (autoCastType != null) {
                if (!typesAreEqual(autoCastType, type1)) expr1.setCastTo(autoCastType);
                if (!typesAreEqual(autoCastType, type2)) expr2.setCastTo(autoCastType);
                return autoCastType;
            } else return null;
        }
    }

    private static boolean typesAreEqual(Type type1, Type type2) {
        if (type1 == null || type2 == null) return false;
        else return (type1.getClass().getSimpleName().equals(type2.getClass().getSimpleName()));
    }

    private static Type AutoCastType(Type type1, Type type2) {
        if (type1 == null || type2 == null) return null;
        if (type1.getClass().getSimpleName().equals(Integer.class.getSimpleName())) return resolveByInt(type2);
        if (type1.getClass().getSimpleName().equals(Double.class.getSimpleName())) return resolveByDouble(type2);
        if (type1.getClass().getSimpleName().equals(Char.class.getSimpleName())) return resolveByChar(type2);
        if (type1.getClass().getSimpleName().equals(Boolean.class.getSimpleName())) return resolveByBoolean(type2);
        if (type1.getClass().getSimpleName().equals(Array.class.getSimpleName()))
            if (type1.getClass().getSimpleName().equals(type2.getClass().getSimpleName()))
                return type1;
            else return null;
        return null;
    }

    private static Type resolveByInt(Type type) {
        if (type.getClass().getSimpleName().equals(Integer.class.getSimpleName())) return new Integer();
        if (type.getClass().getSimpleName().equals(Double.class.getSimpleName())) return new Double();
        if (type.getClass().getSimpleName().equals(Char.class.getSimpleName())) return new Integer();
        if (type.getClass().getSimpleName().equals(Boolean.class.getSimpleName())) return null;
        if (type.getClass().getSimpleName().equals(Array.class.getSimpleName())) return null;
        return null;
    }

    private static Type resolveByDouble(Type type) {
        if (type.getClass().getSimpleName().equals(Integer.class.getSimpleName())) return new Double();
        if (type.getClass().getSimpleName().equals(Double.class.getSimpleName())) return new Double();
        if (type.getClass().getSimpleName().equals(Char.class.getSimpleName())) return new Double();
        if (type.getClass().getSimpleName().equals(Boolean.class.getSimpleName())) return null;
        if (type.getClass().getSimpleName().equals(Array.class.getSimpleName())) return null;
        return null;
    }

    private static Type resolveByChar(Type type) {
        if (type.getClass().getSimpleName().equals(Integer.class.getSimpleName())) return new Integer();
        if (type.getClass().getSimpleName().equals(Double.class.getSimpleName())) return new Double();
        if (type.getClass().getSimpleName().equals(Char.class.getSimpleName())) return new Char();
        if (type.getClass().getSimpleName().equals(Boolean.class.getSimpleName())) return null;
        if (type.getClass().getSimpleName().equals(Array.class.getSimpleName())) return null;
        return null;
    }

    private static Type resolveByBoolean(Type type) {
        if (type.getClass().getSimpleName().equals(Integer.class.getSimpleName())) return null;
        if (type.getClass().getSimpleName().equals(Double.class.getSimpleName())) return null;
        if (type.getClass().getSimpleName().equals(Char.class.getSimpleName())) return new Char();
        if (type.getClass().getSimpleName().equals(Boolean.class.getSimpleName())) return new Boolean();
        if (type.getClass().getSimpleName().equals(Array.class.getSimpleName())) return null;

        return null;
    }

    //это тип узлов, которые мы ищем. чтобы узнать тип переменной, надо найти ее объявление
    // переменная можеть существовать без объявления
    //если она используется как счетчик в for. поэтому иногда мы не найдем declaration
    private static Type exploreType(VariableReference variableReference) {
        Optional<Declaration> declaration = Utils.getAllVisibleNodes(variableReference, Declaration.class)
                .stream().filter(decl -> decl.getVariable().getVarName().equals(variableReference.getVarName()))
                .findFirst();
        if (declaration.isPresent()) return declaration.get().getType();
        else {
            // нужно сначала найти нужный for
            Optional<ForLoop> forLoop = Utils.getAllVisibleNodes(variableReference, ForLoop.class)
                    .stream().filter(x -> x.getIterator().getVarName()
                            .equals(variableReference.getVarName()))
                    .findFirst();
            if (forLoop.isPresent()) {
                if (getType(forLoop.get().getIterable()) instanceof Array) {//перебираемая переменная - массив?
                    //тип переменной - это тип элемента массива
                    return ((Array) getType(forLoop.get().getIterable())).getType();
                }
            }
            //мы нашли все видимые циклы. но мы не знаем, в каком из них нужная нам переменная. ищем именно тот, в
            // котором название переменной такое
            // стопе  почему get 0 ? 1 же. 1 это что перебираем. 0 это какая переменная
            // ясно  тгда почемы ты тип сравниваешь с className это не я, там было Value почему className
            //у тебя className это название переменной
            //почему просто не посмотерть тип  get(0)? потому что мы его ищем разве не надо пистаь тип ?нет в котлине нет
            // for (i in Array<Int>())
        }
        return null;
    }
}
