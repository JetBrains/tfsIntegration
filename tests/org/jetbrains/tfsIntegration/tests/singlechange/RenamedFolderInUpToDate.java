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

package org.jetbrains.tfsIntegration.tests.singlechange;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class RenamedFolderInUpToDate extends SingleChangeTestCase {

  private FilePath myOriginalFolder;
  private FilePath myRenamedFolder;

  @Override
  protected void preparePaths() {
    myOriginalFolder = getChildPath(mySandboxRoot, "Original");
    myRenamedFolder = getChildPath(mySandboxRoot, "Renamed");
  }

  @Override
  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalFolder, myRenamedFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedFolder, 0);
  }

  @Override
  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalFolder, 0);
  }

  @Override
  protected void checkOriginalStateAfterRollback() throws VcsException {
    checkOriginalStateAfterUpdate();
  }

  @Override
  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedFolder, 0);
  }

  @Override
  protected void makeOriginalState() {
    createDirInCommand(myOriginalFolder);
  }

  @Override
  protected void makeChildChange() {
    rename(myOriginalFolder, myRenamedFolder.getName());
  }

  @Override
  @Nullable
  protected Change getPendingChildChange() throws VcsException {
    return getChanges().getMoveChange(myOriginalFolder, myRenamedFolder);
  }

  @Override
  @Test
  public void doTest() throws VcsException, IOException {
    super.doTest();
  }
}