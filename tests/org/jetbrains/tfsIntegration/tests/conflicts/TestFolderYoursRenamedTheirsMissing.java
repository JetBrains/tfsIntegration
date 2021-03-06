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

public class TestFolderYoursRenamedTheirsMissing extends TestFolderConflict {

  private FilePath myBaseFolder;
  private FilePath myYoursFolder;

  @Override
  protected boolean canMerge() {
    return false;
  }

  @Override
  protected void preparePaths() {
    myBaseFolder = getChildPath(mySandboxRoot, BASE_FOLDERNAME);
    myYoursFolder = getChildPath(mySandboxRoot, YOURS_FOLDERNAME);
  }

  @Override
  protected void prepareBaseRevision() {
    createDirInCommand(myBaseFolder);
  }

  @Override
  protected void prepareTargetRevision() {
    deleteFileInCommand(myBaseFolder);
  }

  @Override
  protected void makeLocalChanges() {
    rename(myBaseFolder, YOURS_FOLDERNAME);
  }

  @Override
  protected void checkResolvedYoursState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myBaseFolder, myYoursFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myYoursFolder, 0);
  }

  @Override
  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 0);
  }

  @Override
  protected void checkResolvedMergeState() {
    Assert.fail("can't merge");
  }

  @Override
  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(new ChangeTypeMask(conflict.getYchg()).containsOnly(ChangeType_type0.Rename));
    Assert.assertTrue(new ChangeTypeMask(conflict.getBchg()).containsOnly(ChangeType_type0.Delete));
    Assert.assertEquals(myYoursFolder, VersionControlPath.getFilePath(conflict.getSrclitem(), true));
    Assert.assertNull(conflict.getTgtlitem());
    Assert.assertEquals(findServerPath(myYoursFolder), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myYoursFolder), conflict.getYsitemsrc());
    Assert.assertEquals(findServerPath(myBaseFolder), conflict.getBsitem());
    Assert.assertEquals(findServerPath(myBaseFolder), conflict.getTsitem());
  }

  @Override
  @Nullable
  protected String mergeName() {
    Assert.fail("not supported");
    return null;
  }

  @Override
  @Nullable
  protected String mergeContent() {
    Assert.fail("not supported");
    return null;
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
