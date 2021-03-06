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

package org.jetbrains.tfsIntegration.tests.conflicts;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ChangeType_type0;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Conflict;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ChangeTypeMask;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestFileYoursRenamedModifiedTheirsRenamedModified extends TestFileConflict {

  private FilePath myBaseFile;
  private FilePath myYoursFile;
  private FilePath myTheirsFile;
  private FilePath myMergedFile;

  @Override
  protected boolean canMerge() {
    return true;
  }

  @Override
  protected void preparePaths() {
    myBaseFile = getChildPath(mySandboxRoot, BASE_FILENAME);
    myYoursFile = getChildPath(mySandboxRoot, YOURS_FILENAME);
    myTheirsFile = getChildPath(mySandboxRoot, THEIRS_FILENAME);
    myMergedFile = getChildPath(mySandboxRoot, MERGED_FILENAME);
  }

  @Override
  protected void prepareBaseRevision() {
    createFileInCommand(myBaseFile, BASE_CONTENT);
  }

  @Override
  protected void prepareTargetRevision() throws VcsException, IOException {
    rename(myBaseFile, THEIRS_FILENAME);
    editFiles(myTheirsFile);
    setFileContent(myTheirsFile, THEIRS_CONTENT);
  }

  @Override
  protected void makeLocalChanges() throws IOException, VcsException {
    rename(myBaseFile, YOURS_FILENAME);
    editFiles(myYoursFile);
    setFileContent(myYoursFile, YOURS_CONTENT);
  }

  @Override
  protected void checkResolvedYoursState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myTheirsFile, myYoursFile, THEIRS_CONTENT, YOURS_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFile(myYoursFile, YOURS_CONTENT, true);
  }

  @Override
  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFile(myTheirsFile, THEIRS_CONTENT, false);
  }

  @Override
  protected void checkResolvedMergeState() throws VcsException {
    getChanges().assertTotalItems(1);

    getChanges().assertRenamedOrMoved(myTheirsFile, myMergedFile, THEIRS_CONTENT, MERGED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFile(myMergedFile, MERGED_CONTENT, true);
  }

  @Override
  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(new ChangeTypeMask(conflict.getYchg()).containsOnly(ChangeType_type0.Edit, ChangeType_type0.Rename));
    Assert.assertTrue(new ChangeTypeMask(conflict.getBchg()).containsOnly(ChangeType_type0.Edit, ChangeType_type0.Rename));
    Assert.assertEquals(myYoursFile, VersionControlPath.getFilePath(conflict.getSrclitem(), false));
    Assert.assertEquals(myYoursFile, VersionControlPath.getFilePath(conflict.getTgtlitem(), false));
    Assert.assertEquals(findServerPath(myYoursFile), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myYoursFile), conflict.getYsitemsrc());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getBsitem());
    Assert.assertEquals(findServerPath(myTheirsFile), conflict.getTsitem());
  }

  @Override
  @Nullable
  protected String mergeName() throws TfsException {
    return findServerPath(myMergedFile);
  }


  @Override
  @Nullable
  protected String mergeContent() {
    return MERGED_CONTENT;
  }

  @Override
  @Nullable
  protected String getExpectedBaseContent() {
    return BASE_CONTENT;
  }

  @Override
  @Nullable
  protected String getExpectedYoursContent() {
    return YOURS_CONTENT;
  }

  @Override
  @Nullable
  protected String getExpectedTheirsContent() {
    return THEIRS_CONTENT;
  }

  @Override
  @Test
  public void testAcceptYours() throws VcsException, IOException {
    super.testAcceptYours();
  }

  @Override
  @Test
  public void testAcceptTheirs() throws VcsException, IOException {
    super.testAcceptTheirs();
  }

  @Override
  @Test
  public void testAcceptMerge() throws VcsException, IOException {
    super.testAcceptMerge();
  }
}
