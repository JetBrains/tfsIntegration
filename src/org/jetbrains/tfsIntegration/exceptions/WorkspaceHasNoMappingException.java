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

package org.jetbrains.tfsIntegration.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import java.text.MessageFormat;


public class WorkspaceHasNoMappingException extends TfsException {

  private final @NotNull WorkspaceInfo myWorkspace;


  public WorkspaceHasNoMappingException(final @NotNull WorkspaceInfo workspace) {
    myWorkspace = workspace;
  }

  @Override
  public String getMessage() {
    return MessageFormat
      .format("Mappings for workspace ''{0}'' were modified on server. Please review your mapping settings before you continue working.",
              myWorkspace.getName());
  }
}
