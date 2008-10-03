/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is HTML Parser C++ Translator code.
 *
 * The Initial Developer of the Original Code is
 * Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Henri Sivonen <hsivonen@iki.fi>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package nu.validator.htmlparser.cpptranslate;

import java.util.LinkedList;
import java.util.List;

import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;

public class HVisitor extends CppVisitor {

    private enum Visibility {
        NONE, PRIVATE, PUBLIC, PROTECTED,
    }

    private Visibility previousVisibility = Visibility.NONE;

    private List<String> defines = new LinkedList<String>();

    private SourcePrinter arrayInitPrinter = new SourcePrinter();
    private SourcePrinter mainPrinterHolder;

    /**
     * @see nu.validator.htmlparser.cpptranslate.CppVisitor#printMethodNamespace()
     */
    @Override protected void printMethodNamespace() {
    }

    public HVisitor(CppTypes cppTypes, SymbolTable symbolTable) {
        super(cppTypes, symbolTable);
    }

    /**
     * @see nu.validator.htmlparser.cpptranslate.CppVisitor#startClassDeclaration()
     */
    @Override protected void startClassDeclaration() {
        printer.print("class ");
        printer.printLn(className);
        printer.printLn("{");
        printer.indent();
        printer.indent();
    }

    /**
     * @see nu.validator.htmlparser.cpptranslate.CppVisitor#endClassDeclaration()
     */
    @Override protected void endClassDeclaration() {
        printer.unindent();
        printer.unindent();
        printer.printLn("};");
        printer.printLn();

        printer.print(arrayInitPrinter.getSource());
        printer.printLn();
        
        for (String define : defines) {
            printer.printLn(define);
        }
    }

    /**
     * @see nu.validator.htmlparser.cpptranslate.CppVisitor#printMethodBody(japa.parser.ast.stmt.BlockStmt, java.lang.Object)
     */
    @Override protected void printMethodBody(BlockStmt n, Object arg) {
        printer.printLn(";");
    }

    /**
     * @see nu.validator.htmlparser.cpptranslate.CppVisitor#printModifiers(int)
     */
    @Override protected void printModifiers(int modifiers) {
        if (ModifierSet.isPrivate(modifiers)) {
            if (previousVisibility != Visibility.PRIVATE) {
                printer.unindent();
                printer.printLn("private:");
                printer.indent();
                previousVisibility = Visibility.PRIVATE;
            }
        } else if (ModifierSet.isProtected(modifiers)) {
            if (previousVisibility != Visibility.PROTECTED) {
                printer.unindent();
                printer.printLn("protected:");
                printer.indent();
                previousVisibility = Visibility.PROTECTED;
            }
        } else {
            if (previousVisibility != Visibility.PUBLIC) {
                printer.unindent();
                printer.printLn("public:");
                printer.indent();
                previousVisibility = Visibility.PUBLIC;
            }
        }
        if (ModifierSet.isStatic(modifiers)) {
            printer.print("static ");
        }
    }

    /**
     * @see nu.validator.htmlparser.cpptranslate.CppVisitor#fieldDeclaration(japa.parser.ast.body.FieldDeclaration, java.lang.Object)
     */
    @Override protected void fieldDeclaration(FieldDeclaration n, Object arg) {
        int modifiers = n.getModifiers();
        List<VariableDeclarator> variables = n.getVariables();
        VariableDeclarator declarator = variables.get(0);
        if (ModifierSet.isStatic(modifiers) && ModifierSet.isFinal(modifiers)
                && n.getType() instanceof PrimitiveType) {
            PrimitiveType type = (PrimitiveType) n.getType();
            if (type.getType() != PrimitiveType.Primitive.Int) {
                throw new IllegalStateException(
                        "Only int constant #defines supported.");
            }
            if (variables.size() != 1) {
                throw new IllegalStateException(
                        "More than one variable declared by one declarator.");
            }
            String name = javaClassName + "." + declarator.getId().getName();
            String value = declarator.getInit().toString();
            String longName = definePrefix + declarator.getId().getName();
            if (symbolTable.cppDefinesByJavaNames.containsKey(name)) {
                throw new IllegalStateException(
                        "Duplicate #define constant local name: " + name);
            }
            symbolTable.cppDefinesByJavaNames.put(name, longName);
            defines.add("#define " + longName + " " + value);
        } else {
            if (ModifierSet.isStatic(modifiers)) {
                inFieldDeclarator = true;
            }
            boolean isConst = false;
            if (n.getType() instanceof ReferenceType) {
                ReferenceType rt = (ReferenceType) n.getType();
                currentArrayCount = rt.getArrayCount();
                if (currentArrayCount > 0
                        && (rt.getType() instanceof PrimitiveType) && declarator.getInit() != null && noLength()) {
                    if (!ModifierSet.isStatic(modifiers)) {
                        throw new IllegalStateException(
                                "Non-static array case not supported here." + declarator);
                    }
                    isConst = true;
                    mainPrinterHolder = printer;
                    printer = arrayInitPrinter;
                    n.getType().accept(this, arg);
                    arrayInitPrinter.print(" const ");
                    arrayInitPrinter.print(className);
                    arrayInitPrinter.print("::");
                    declarator.getId().accept(this, arg);

                    arrayInitPrinter.print(" = ");                    
                    
                    declarator.getInit().accept(this, arg);
                    
                    printer.printLn(";");                    
                    printer = mainPrinterHolder;
                }
            }
            printModifiers(modifiers);
            if (isConst) {
                printer.print("const ");
            }
            n.getType().accept(this, arg);
            printer.print(" ");
            declarator.getId().accept(this, arg);
            printer.printLn(";");
            currentArrayCount = 0;
            inFieldDeclarator = false;
        }
    }

}