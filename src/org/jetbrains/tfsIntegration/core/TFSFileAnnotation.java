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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspectAdapter;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import git4idea.annotate.AnnotationTooltipBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsRevisionNumber;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;

public class TFSFileAnnotation extends FileAnnotation {
  private final TFSVcs myVcs;
  private final WorkspaceInfo myWorkspace;
  private final String myAnnotatedContent;
  private final VcsFileRevision[] myLineRevisions;
  private final VirtualFile myFile;

  private final LineAnnotationAspect REVISION_ASPECT = new TFSAnnotationAspect(TFSAnnotationAspect.REVISION, false) {
    @Override
    public String getValue(int lineNumber) {
      VcsFileRevision fileRevision = getLineRevision(lineNumber);
      if (fileRevision == null) return "";

      return ((TfsRevisionNumber)fileRevision.getRevisionNumber()).getChangesetString();
    }
  };

  private final LineAnnotationAspect DATE_ASPECT = new TFSAnnotationAspect(TFSAnnotationAspect.DATE, true) {
    @Override
    public String getValue(int lineNumber) {
      VcsFileRevision fileRevision = getLineRevision(lineNumber);
      if (fileRevision == null) return "";

      return FileAnnotation.formatDate(fileRevision.getRevisionDate());
    }
  };

  private final LineAnnotationAspect AUTHOR_ASPECT = new TFSAnnotationAspect(TFSAnnotationAspect.AUTHOR, true) {
    @Override
    public String getValue(int lineNumber) {
      VcsFileRevision fileRevision = getLineRevision(lineNumber);
      if (fileRevision == null) return "";

      return TfsUtil.getNameWithoutDomain(fileRevision.getAuthor());
    }
  };

  private final TFSVcs.RevisionChangedListener myListener = new TFSVcs.RevisionChangedListener() {
    @Override
    public void revisionChanged() {
      try {
        GuiUtils.runOrInvokeAndWait(() -> TFSFileAnnotation.this.close());
      }
      catch (InvocationTargetException e) {
        // ignore
      }
      catch (InterruptedException e) {
        // ignore
      }
    }
  };

  public TFSFileAnnotation(final TFSVcs vcs,
                           final WorkspaceInfo workspace,
                           final String annotatedContent,
                           final VcsFileRevision[] lineRevisions, VirtualFile file) {
    super(vcs.getProject());
    myVcs = vcs;
    myWorkspace = workspace;
    myAnnotatedContent = annotatedContent;
    myLineRevisions = lineRevisions;
    myFile = file;
    myVcs.addRevisionChangedListener(myListener);
  }

  @Override
  public void dispose() {
    myVcs.removeRevisionChangedListener(myListener);
  }

  @Override
  public String getAnnotatedContent() {
    return myAnnotatedContent;
  }

  @Override
  public LineAnnotationAspect[] getAspects() {
    return new LineAnnotationAspect[]{REVISION_ASPECT, DATE_ASPECT, AUTHOR_ASPECT};
  }

  @Nullable
  private VcsFileRevision getLineRevision(int lineNumber) {
    if (lineNumber < 0 || lineNumber >= myLineRevisions.length) return null;
    return myLineRevisions[lineNumber];
  }

  @Nullable
  @Override
  public String getToolTip(int lineNumber) {
    return getToolTip(lineNumber, false);
  }

  @Nullable
  @Override
  public String getHtmlToolTip(int lineNumber) {
    return getToolTip(lineNumber, true);
  }

  @Nullable
  private String getToolTip(int lineNumber, boolean asHtml) {
    VcsFileRevision fileRevision = getLineRevision(lineNumber);
    if (fileRevision == null) return null;

    String commitMessage = fileRevision.getCommitMessage() == null ? "(no comment)" : fileRevision.getCommitMessage();

    return AnnotationTooltipBuilder.buildSimpleTooltip(getProject(), asHtml, "Changeset",
                                                       ((TfsRevisionNumber)fileRevision.getRevisionNumber()).getChangesetString(),
                                                       commitMessage);
  }

  @Override
  @Nullable
  public VcsRevisionNumber getLineRevisionNumber(final int lineNumber) {
    VcsFileRevision fileRevision = getLineRevision(lineNumber);
    if (fileRevision == null) return null;

    return fileRevision.getRevisionNumber();
  }

  @Override
  public Date getLineDate(int lineNumber) {
    VcsFileRevision fileRevision = getLineRevision(lineNumber);
    if (fileRevision == null) return null;

    return fileRevision.getRevisionDate();
  }

  @Override
  public List<VcsFileRevision> getRevisions() {
    Set<VcsFileRevision> set = new HashSet<>(Arrays.asList(myLineRevisions));
    List<VcsFileRevision> result = new ArrayList<>(set);
    Collections.sort(result, REVISION_COMPARATOR);
    return result;
  }

  @Override
  public int getLineCount() {
    return myLineRevisions.length;
  }

  private static final Comparator<VcsFileRevision> REVISION_COMPARATOR =
    (revision1, revision2) -> -1 * revision1.getRevisionNumber().compareTo(revision2.getRevisionNumber());

  private abstract class TFSAnnotationAspect extends LineAnnotationAspectAdapter {
    TFSAnnotationAspect(String id, boolean showByDefault) {
      super(id, showByDefault);
    }

    @Override
    protected void showAffectedPaths(int lineNum) {
      final VcsFileRevision revision = getLineRevision(lineNum);
      if (revision == null) return;

      final int changeset = ((VcsRevisionNumber.Int)revision.getRevisionNumber()).getValue();
      final CommittedChangeList changeList =
        new TFSChangeList(myWorkspace, changeset, revision.getAuthor(), revision.getRevisionDate(), revision.getCommitMessage(), myVcs);
      String changesetString = ((TfsRevisionNumber)revision.getRevisionNumber()).getChangesetString();
      final String progress = MessageFormat.format("Loading changeset {0}...", changesetString);
      ProgressManager.getInstance().runProcessWithProgressSynchronously((Runnable)() -> changeList.getChanges(), progress, false, myVcs.getProject());
      final String title = MessageFormat.format("Changeset {0}", changesetString);
      AbstractVcsHelper.getInstance(myVcs.getProject()).showChangesListBrowser(changeList, title);
    }
  }

  @Nullable
  @Override
  public VcsRevisionNumber getCurrentRevision() {
    return null;
  }

  @Override
  public VcsKey getVcsKey() {
    return TFSVcs.getKey();
  }

  @Override
  public VirtualFile getFile() {
    return myFile;
  }
}
