package org.jetbrains.tfsIntegration.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.tfsIntegration.ui.ManageWorkspacesDialog;

public class TfsEditConfigurationAction extends DumbAwareAction {

  public TfsEditConfigurationAction() {
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(project != null);
    }
    else {
      e.getPresentation().setEnabled(project != null);
    }
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    ManageWorkspacesDialog d = new ManageWorkspacesDialog(project);
    d.show();
  }
}
