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
package org.jetbrains.tfsIntegration.core.tfs.conflicts;

import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.InvalidDiffRequestException;
import com.intellij.diff.merge.MergeRequest;
import com.intellij.diff.merge.MergeResult;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.merge.MergeDialogCustomizer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Conflict;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.ui.ContentTriplet;

import java.util.List;

public class DialogContentMerger implements ContentMerger {
  @Override
  public boolean mergeContent(Conflict conflict, ContentTriplet contentTriplet, Project project, final VirtualFile localFile, String localPath,
                              VcsRevisionNumber serverVersion) throws VcsException {
    TFSVcs.assertTrue(localFile.isWritable(), localFile.getPresentableUrl() + " must be writable");

    List<byte[]> contents = ContainerUtil.list(contentTriplet.localContent,
                                               contentTriplet.baseContent,
                                               contentTriplet.serverContent);

    MergeDialogCustomizer c = new MergeDialogCustomizer();
    String title = c.getMergeWindowTitle(localFile);
    List<String> contentTitles = ContainerUtil.list(c.getLeftPanelTitle(localFile),
                                                    c.getCenterPanelTitle(localFile),
                                                    c.getRightPanelTitle(localFile, serverVersion));


    try {
      Ref<MergeResult> resultRef = new Ref<>(MergeResult.CANCEL);
      MergeRequest request = DiffRequestFactory.getInstance().createMergeRequest(project, localFile, contents, title, contentTitles,
                                                                                 mergeResult -> resultRef.set(mergeResult));
      DiffManager.getInstance().showMerge(project, request);
      return resultRef.get() != MergeResult.CANCEL;
    }
    catch (InvalidDiffRequestException e) {
      throw new VcsException(e);
    }
  }
}
