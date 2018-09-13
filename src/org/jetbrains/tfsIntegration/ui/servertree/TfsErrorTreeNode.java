package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

public class TfsErrorTreeNode extends SimpleNode {
  private final String myMessage;

  public TfsErrorTreeNode(SimpleNode parent, String message) {
    super(parent);
    myMessage = message;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.addText(myMessage, getErrorAttributes());
    presentation.setIcon(PlatformIcons.ERROR_INTRODUCTION_ICON);
  }

  @NotNull
  @Override
  public SimpleNode[] getChildren() {
    return NO_CHILDREN;
  }

  public String getMessage() {
    return myMessage;
  }
}
