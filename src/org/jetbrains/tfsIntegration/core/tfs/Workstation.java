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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.util.Function;
import com.intellij.util.Functions;
import com.intellij.util.containers.ContainerUtil;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.config.TfsServerConnectionHelper;
import org.jetbrains.tfsIntegration.core.TfsSdkManager;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.exceptions.DuplicateMappingException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.WorkspaceHasNoMappingException;
import org.jetbrains.tfsIntegration.xmlutil.XmlUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.intellij.util.containers.ContainerUtil.newArrayList;
import static org.jetbrains.tfsIntegration.core.tfs.XmlConstants.*;

public class Workstation {

  // to be used in tests
  public static boolean PRESERVE_CONFIG_FILE = false;

  private static final Logger LOG = Logger.getInstance(Workstation.class.getName());

  @NotNull private final List<ServerInfo> myServerInfos;

  private @Nullable Ref<FilePath> myDuplicateMappedPath;

  private static String ourComputerName;

  private Workstation() {
    myServerInfos = loadCache();
  }

  private static class WorkstationHolder {
    private static final Workstation ourInstance = new Workstation();
  }

  @NotNull
  public static Workstation getInstance() {
    return WorkstationHolder.ourInstance;
  }

  @NotNull
  public List<ServerInfo> getServers() {
    return Collections.unmodifiableList(myServerInfos);
  }

  @Nullable
  public ServerInfo getServer(@NotNull URI uri) {
    return ContainerUtil.find(getServers(), serverInfo -> serverInfo.getUri().equals(uri));
  }

  @NotNull
  private List<WorkspaceInfo> getAllWorkspacesForCurrentOwnerAndComputer(boolean showLoginIfNoCredentials) {
    List<WorkspaceInfo> result = newArrayList();
    for (final ServerInfo server : getServers()) {
      if (showLoginIfNoCredentials && server.getQualifiedUsername() == null) {
        try {
          TfsServerConnectionHelper.ensureAuthenticated(null, server.getUri(), false);
        }
        catch (TfsException e) {
          continue;
        }
      }
      result.addAll(server.getWorkspacesForCurrentOwnerAndComputer());
    }
    return result;
  }

  @NotNull
  private static List<ServerInfo> loadCache() {
    // TODO: validate against schema
    File cacheFile = getCacheFile(true);
    if (cacheFile != null) {
      try {
        WorkstationCacheReader reader = new WorkstationCacheReader();
        XmlUtil.parseFile(cacheFile, reader);
        return reader.getServers();
      }
      catch (Exception e) {
        LOG.info("Cannot read workspace cache", e);
      }
    }
    return newArrayList();
  }


  @Nullable
  private static File getCacheFile(boolean existingOnly) {
    if (PRESERVE_CONFIG_FILE) {
      return null;
    }

    File cacheFile = TfsSdkManager.getInstance().getCacheFile();
    return (cacheFile.exists() || !existingOnly) ? cacheFile : null;
  }

  void update() {
    invalidateDuplicateMappedPath();

    File cacheFile = getCacheFile(false);
    if (cacheFile != null) {
      if (!cacheFile.getParentFile().exists()) {
        cacheFile.getParentFile().mkdirs();
      }
      try {
        Element serversElement = new Element(SERVERS);

        for (ServerInfo serverInfo : getServers()) {
          Element serverInfoElement = new Element(SERVER_INFO)
            .setAttribute(URI_ATTR, serverInfo.getUri().toString())
            .setAttribute(GUID_ATTR, serverInfo.getGuid());

          serversElement.addContent(serverInfoElement);

          for (WorkspaceInfo workspaceInfo : serverInfo.getWorkspaces()) {
            Element workspaceInfoElement = new Element(WORKSPACE_INFO)
              .setAttribute(COMPUTER_ATTR, workspaceInfo.getComputer())
              .setAttribute(OWNER_NAME_ATTR, workspaceInfo.getOwnerName())
              .setAttribute(TIMESTAMP_ATTR, ConverterUtil.convertToString(workspaceInfo.getTimestamp()))
              .setAttribute(NAME_ATTR, workspaceInfo.getName())
              .setAttribute(IS_LOCAL_WORKSPACE_ATTR, String.valueOf(workspaceInfo.isLocal()))
              .setAttribute(OPTIONS_ATTR, String.valueOf(workspaceInfo.getOptions()))
              // "comment" and "ownerDisplayName" attributes are required (otherwise Eclipse TFS plug-in fails to read cache)
              .setAttribute(COMMENT_ATTR, StringUtil.notNullize(workspaceInfo.getComment()))
              .setAttribute(OWNER_DISPLAY_NAME_ATTR, StringUtil.notNullize(workspaceInfo.getOwnerDisplayName()));
            setIfNotNull(workspaceInfoElement, SECURITY_TOKEN_ATTR, workspaceInfo.getSecurityToken());

            addItems(workspaceInfoElement, MAPPED_PATHS, MAPPED_PATH, PATH_ATTR, workspaceInfo.getWorkingFoldersCached(),
                     folderInfo -> folderInfo.getLocalPath().getPresentableUrl());

            addItems(workspaceInfoElement, OWNER_ALIASES, OWNER_ALIAS, OWNER_ALIAS_ATTR,
                     workspaceInfo.getOwnerAliases(), Functions.TO_STRING());

            serverInfoElement.addContent(workspaceInfoElement);
          }
        }

        Document document = new Document().setRootElement(
          new Element(ROOT).addContent(serversElement));

        saveDocument(cacheFile, document);
      }
      catch (IOException e) {
        LOG.info("Cannot update workspace cache", e);
      }
    }
  }

  private static void saveDocument(@NotNull File cacheFile, @NotNull Document document) throws IOException {
    OutputStream stream = new BufferedOutputStream(new FileOutputStream(cacheFile));
    try {
      XMLOutputter o = JDOMUtil.createOutputter("\n");
      o.setFormat(o.getFormat().setOmitDeclaration(true));
      o.output(document, stream);
    }
    catch (NullPointerException e) {
      LOG.warn(e);
    }
    finally {
      stream.close();
    }
  }

  private static <T> void addItems(@NotNull Element parentElement,
                                   @NotNull String elementName,
                                   @NotNull String itemElementName,
                                   @NotNull String itemAttributeName,
                                   @NotNull List<T> items,
                                   @NotNull Function<T, String> valueProvider) {
    Element element = new Element(elementName);
    parentElement.addContent(element);

    for (T item : items) {
      element.addContent(new Element(itemElementName).setAttribute(itemAttributeName, StringUtil.notNullize(valueProvider.fun(item))));
    }
  }

  private static void setIfNotNull(@NotNull Element element, @NotNull String attributeName, @Nullable String value) {
    if (value != null) {
      element.setAttribute(attributeName, value);
    }
  }

  public void addServer(final ServerInfo serverInfo) {
    myServerInfos.add(serverInfo);
    update();
  }

  public void removeServer(final ServerInfo serverInfo) {
    myServerInfos.remove(serverInfo);

    TFSConfigurationManager.getInstance().remove(serverInfo.getUri());
    update();
  }

  public synchronized static String getComputerName() {
    if (ourComputerName == null) {
      try {
        InetAddress address = InetAddress.getLocalHost();
        String hostName = address.getHostName();

        // Ideally, we should return an equivalent of .NET's Environment.MachineName which
        // "Gets the NetBIOS name of this local computer."
        // (see http://msdn.microsoft.com/en-us/library/system.environment.machinename.aspx)
        // All we can do is just strip DNS suffix

        int i = hostName.indexOf('.');
        if (i != -1) {
          hostName = hostName.substring(0, i);
        }
        ourComputerName = hostName;
      }
      catch (UnknownHostException e) {
        // must never happen
        throw new RuntimeException("Cannot retrieve host name.");
      }
    }
    return ourComputerName;
  }

  @NotNull
  public Collection<WorkspaceInfo> findWorkspacesCached(final @NotNull FilePath localPath, boolean considerChildMappings) {
    // try cached working folders first
    Collection<WorkspaceInfo> result = newArrayList();
    for (WorkspaceInfo workspace : getAllWorkspacesForCurrentOwnerAndComputer(false)) {
      if (workspace.hasMappingCached(localPath, considerChildMappings)) {
        result.add(workspace);
        if (!considerChildMappings) {
          // optimization: same local path can't be mapped in different workspaces, so don't process other workspaces
          break;
        }
      }
    }
    return result;
  }

  @NotNull
  public Collection<WorkspaceInfo> findWorkspaces(final @NotNull FilePath localPath,
                                                  boolean considerChildMappings,
                                                  Object projectOrComponent) throws TfsException {
    checkDuplicateMappings();
    final Collection<WorkspaceInfo> resultCached = findWorkspacesCached(localPath, considerChildMappings);
    if (!resultCached.isEmpty()) {
      // given path is mapped according to cached mapping info -> reload and check with server info
      for (WorkspaceInfo workspace : resultCached) {
        if (!workspace.hasMapping(localPath, considerChildMappings, projectOrComponent)) {
          throw new WorkspaceHasNoMappingException(workspace);
        }
      }
      return resultCached;
    }
    else {
      // TODO: exclude servers that are unavailable during current application run
      // not found in cached info, but workspaces may be out of date -> try to search all the workspaces reloaded
      Collection<WorkspaceInfo> result = newArrayList();
      Collection<ServerInfo> serversToSkip = newArrayList();
      for (WorkspaceInfo workspace : getAllWorkspacesForCurrentOwnerAndComputer(true)) {
        if (serversToSkip.contains(workspace.getServer())) {
          // if server is somehow unavailable, don't try every workspace on it
          continue;
        }
        try {
          if (workspace.hasMapping(localPath, considerChildMappings, projectOrComponent)) {
            result.add(workspace);
            if (!considerChildMappings) {
              // optmimization: same local path can't be mapped in different workspaces, so don't process other workspaces
              return result;
            }
          }
        }
        catch (TfsException e) {
          // if some server failed, try next one, otherwise user will get strange error messages
          serversToSkip.add(workspace.getServer());
        }
      }
      return result;
    }
  }

  public void checkDuplicateMappings() throws DuplicateMappingException {
    if (myDuplicateMappedPath == null) {
      myDuplicateMappedPath = Ref.create(findDuplicateMappedPath());
    }
    //noinspection ConstantConditions
    if (!myDuplicateMappedPath.isNull()) {
      //noinspection ConstantConditions
      throw new DuplicateMappingException(myDuplicateMappedPath.get());
    }
  }

  private void invalidateDuplicateMappedPath() {
    myDuplicateMappedPath = null;
  }

  @Nullable
  private FilePath findDuplicateMappedPath() {
    // don't check duplicate mappings within the same server, server side should take care about this
    Collection<FilePath> otherServersPaths = newArrayList();
    for (ServerInfo server : getServers()) {
      Collection<FilePath> currentServerPaths = newArrayList();
      for (WorkspaceInfo workspace : server.getWorkspacesForCurrentOwnerAndComputer()) {
        for (WorkingFolderInfo workingFolder : workspace.getWorkingFoldersCached()) {
          final FilePath currentServerPath = workingFolder.getLocalPath();
          for (FilePath otherServerPath : otherServersPaths) {
            if (currentServerPath.isUnder(otherServerPath, false)) {
              return currentServerPath;
            }
            if (otherServerPath.isUnder(currentServerPath, false)) {
              return otherServerPath;
            }
          }
          currentServerPaths.add(currentServerPath);
        }
      }
      otherServersPaths.addAll(currentServerPaths);
    }
    return null;
  }

}
