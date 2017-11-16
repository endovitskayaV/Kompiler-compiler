package com.end.compiler;

import com.end.compiler.KLexer;
import com.end.compiler.KParser;
import io.bretty.console.tree.TreePrinter;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.Tree;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import javax.swing.JFrame;
import java.io.IOException;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class Main {

    public static void main(String[] args) {

//        ArrayList<Integer> list=new ArrayList<>();
//        list.add(1);
//        list.add(3);
//        list.add(4);
//        list.add(5);
//        list.add(1);list.add(5);
//        list.stream().filter
//                (x-> Collections.frequency(list,x)>1).sorted();
//               // .forEach(System.out::println);
//
        CharStream stream = null;
        try {
            stream = CharStreams.fromFileName("code.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        KLexer kLexer = new KLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(kLexer);
        KParser kParser = new KParser(tokenStream);
        Tree tree = kParser.program();

        showSyntaxTree(tree,kParser);

        Program astTree = ToAst.toAst((KParser.ProgramContext) tree);

        // Analysis.analyze(astTree);

        String outputStr=TreePrinter.toString(astTree);

        String fileName="astTree.txt";
        try(PrintWriter printWriter=new PrintWriter(fileName)) {
            printWriter.write(outputStr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Printed in " + fileName + ":\n" + outputStr);

    }

    private static  void showSyntaxTree(Tree tree, KParser kParser){
        JFrame frame = new JFrame("Syntax tree");
        TreeViewer treeViewer = new TreeViewer(Arrays.asList(kParser.getRuleNames()), tree);
        treeViewer.setScale(1.5);
        frame.add(treeViewer);
        frame.setSize(640, 480);
        frame.setVisible(true);
    }
}