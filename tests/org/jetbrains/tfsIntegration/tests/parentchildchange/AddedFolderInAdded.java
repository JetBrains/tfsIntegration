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

// see remarks for AddedFileInAdded test case

@SuppressWarnings({"HardCodedStringLiteral"})
public class AddedFolderInAdded extends ParentChildChangeTestCase {
  private FilePath myAddedParentFolder;
  private FilePath myAddedChildFolder;

  @Override
  protected void preparePaths() {
    myAddedParentFolder = getChildPath(mySandboxRoot, "AddedFolder");
    myAddedChildFolder = getChildPath(myAddedParentFolder, "AddedSubfolder");
  }

  @Override
  protected void checkParentChangePendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    getChanges().assertUnversioned(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder);
    getChanges().assertScheduledForAddition(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    getChanges().assertScheduledForAddition(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder);
    getChanges().assertUnversioned(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 0);
  }

  @Override
  protected void checkParentChangeCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void checkChildChangeCommittedParentPending() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 2
  }

  @Override
  protected void checkParentChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 0);
  }

  @Override
  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder); // see remark 1
    getChanges().assertScheduledForAddition(myAddedChildFolder);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void checkParentChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 0);
  }

  @Override
  protected void checkChildChangeCommitted() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 2
  }

  @Override
  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  @Override
  protected void makeOriginalState() {
  }

  @Override
  protected void makeParentChange() throws VcsException {
    if (myAddedParentFolder.getIOFile().exists()) {
      if (getChanges().isUnversioned(myAddedParentFolder)) {
        scheduleForAddition(myAddedParentFolder);
      }
    }
    else {
      createDirInCommand(myAddedParentFolder);
    }
  }

  @Override
  protected void makeChildChange(ParentChangeState parentChangeState) {
    if (parentChangeState == ParentChangeState.NotDone) {
      myAddedParentFolder.getIOFile().mkdirs();
      refreshAll();
    }

    if (myAddedChildFolder.getIOFile().exists()) {
      scheduleForAddition(myAddedChildFolder);
    }
    else {
      createDirInCommand(myAddedChildFolder);
    }
  }

  @Override
  @Nullable
  protected Change getPendingParentChange() throws VcsException {
    return getChanges().getAddChange(myAddedParentFolder);
  }

  @Override
  protected Change getPendingChildChange(ParentChangeState parentChangeState) throws VcsException {
    return getChanges().getAddChange(myAddedChildFolder);
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