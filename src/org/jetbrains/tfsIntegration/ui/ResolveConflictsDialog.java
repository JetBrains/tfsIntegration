/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Conflict;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;

import javax.swing.*;

public class ResolveConflictsDialog extends DialogWrapper {
  private final ResolveConflictHelper myResolveConflictHelper;

  public ResolveConflictsDialog(final ResolveConflictHelper resolveConflictHelper) {
    super(true);
    myResolveConflictHelper = resolveConflictHelper;
    setTitle("Resolve Conflicts");
    setResizable(true);
    setOKButtonText("Close");
    init();
  }

  @Override
  protected void doOKAction() {
    for (Conflict conflict : myResolveConflictHelper.getConflicts()) {
      myResolveConflictHelper.skip(conflict);
    }
    super.doOKAction();
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    ResolveConflictsForm resolveConflictsForm = new ResolveConflictsForm(myResolveConflictHelper);
    resolveConflictsForm.addListener(new ResolveConflictsForm.Listener() {
      @Override
      public void close() {
        ResolveConflictsDialog.this.close(OK_EXIT_CODE);
      }
    });
    return resolveConflictsForm.getPanel();
  }

  @Override
  @NotNull
  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ResolveConflicts";
  }

}
