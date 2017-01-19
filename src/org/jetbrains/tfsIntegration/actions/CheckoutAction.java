/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.RootsCollection;

import java.util.stream.Stream;

import static com.intellij.openapi.vfs.VfsUtilCore.toVirtualFileArray;
import static com.intellij.vcsUtil.VcsUtil.getVirtualFiles;
import static java.util.Collections.singletonList;

public class CheckoutAction extends AnAction implements DumbAware {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    RootsCollection.VirtualFileRootsCollection roots = new RootsCollection.VirtualFileRootsCollection(getVirtualFiles(e));
    Ref<VcsException> error = Ref.create();

    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        TFSVcs.getInstance(project).getEditFileProvider().editFiles(toVirtualFileArray(roots));
      }
      catch (VcsException ex) {
        error.set(ex);
      }
    }, "Checking out files for edit...", false, project);

    if (!error.isNull()) {
      AbstractVcsHelper.getInstance(project).showErrors(singletonList(error.get()), TFSVcs.TFS_NAME);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile[] files = getVirtualFiles(e);

    e.getPresentation().setEnabled(project != null && files.length != 0 && areNotChangedOrHijacked(project, files));
  }

  private static boolean areNotChangedOrHijacked(@NotNull Project project, @NotNull VirtualFile[] files) {
    FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);

    return Stream.of(files)
      .map(fileStatusManager::getStatus)
      .allMatch(status -> status == FileStatus.NOT_CHANGED || status == FileStatus.HIJACKED);
  }
}
