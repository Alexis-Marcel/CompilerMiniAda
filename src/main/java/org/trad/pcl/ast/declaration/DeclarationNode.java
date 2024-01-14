package org.trad.pcl.ast.declaration;

import org.trad.pcl.ast.ASTNode;

public class DeclarationNode extends ASTNode {
    // Classe de base pour les déclarations
    protected String name;

    public DeclarationNode(String name) {
        this.name = name;
    }
}