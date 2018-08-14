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

package org.jetbrains.tfsIntegration.tests.parentchildchange;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class DeletedFolderInRenamed extends ParentChildChangeTestCase {
  private FilePath myOriginalParentFolder;
  private FilePath myRenamedParentFolder;
  private FilePath myDeletedFolderOriginalFolder;
  private FilePath myDeletedFolderInRenamedFolder;

  @Override
  protected void preparePaths() {
    myOriginalParentFolder = getChildPath(mySandboxRoot, "OriginalFolder");
    myRenamedParentFolder = getChildPath(mySandboxRoot, "RenamedFolder");

    final String filename = "DeletedSubfolder";
    myDeletedFolderOriginalFolder = getChildPath(myOriginalParentFolder, filename);
    myDeletedFolderInRenamedFolder = getChildPath(myRenamedParentFolder, filename);
  }

  @Override
  protected void checkParentChangePendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFolder(myDeletedFolderInRenamedFolder, 0);
  }

  @Override
  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForDeletion(myDeletedFolderOriginalFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 0);
  }

  @Override
  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);
    getChanges().assertScheduledForDeletion(myDeletedFolderInRenamedFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 0);
  }

  @Override
  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 1);
    assertFolder(myDeletedFolderOriginalFolder, 0);
  }

  @Override
  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 1);
    assertFolder(myDeletedFolderOriginalFolder, 0);
  }

  @Override
  protected void checkParentChangeCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForDeletion(myDeletedFolderInRenamedFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 0);
  }

  @Override
  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 0);
  }

  @Override
  protected void checkParentChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFolder(myDeletedFolderInRenamedFolder, 0);
  }

  @Override
  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForDeletion(myDeletedFolderOriginalFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 0);
  }

  @Override
  protected void checkParentChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFolder(myDeletedFolderInRenamedFolder, 0);
  }

  @Override
  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 0);
  }

  @Override
  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 0);
  }

  @Override
  protected void makeOriginalState() {
    createDirInCommand(myOriginalParentFolder);
    createDirInCommand(myDeletedFolderOriginalFolder);
  }

  @Override
  protected void makeParentChange() {
    renameFileInCommand(myOriginalParentFolder, myRenamedParentFolder.getName());
  }

  @Override
  protected void makeChildChange(ParentChangeState parentChangeState) {
    deleteFileInCommand(parentChangeState == ParentChangeState.NotDone ? myDeletedFolderOriginalFolder : myDeletedFolderInRenamedFolder);
  }

  @Override
  @Nullable
  protected Change getPendingParentChange() throws VcsException {
    return getChanges().getMoveChange(myOriginalParentFolder, myRenamedParentFolder);
  }

  @Override
  protected Change getPendingChildChange(ParentChangeState parentChangeState) throws VcsException {
    return getChanges()
      .getDeleteChange(parentChangeState == ParentChangeState.NotDone ? myDeletedFolderOriginalFolder : myDeletedFolderInRenamedFolder);
  }

  @Override
  @Test
  public void testPendingAndRollback() throws VcsException, IOException {
    super.testPendingAndRollback();
  }

  @Override
  @Test
  public void testCommitParentThenChildChanges() throws VcsException, IOException {
    super.testCommitParentThenChildChanges();
  }

  @Override
  @Test
  public void testCommitChildThenParentChanges() throws VcsException, IOException {
    super.testCommitChildThenParentChanges();
  }

  @Override
  @Test
  public void testCommitParentChangesChildPending() throws VcsException, IOException {
    super.testCommitParentChangesChildPending();
  }

  @Override
  @Test
  public void testCommitChildChangesParentPending() throws VcsException, IOException {
    super.testCommitChildChangesParentPending();
  }

}