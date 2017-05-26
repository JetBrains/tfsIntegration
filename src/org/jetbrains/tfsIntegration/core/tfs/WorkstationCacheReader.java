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

import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TfsBeansHolder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.List;

import static org.jetbrains.tfsIntegration.core.tfs.XmlConstants.*;

class WorkstationCacheReader extends DefaultHandler {

  @NotNull private final List<ServerInfo> myServerInfos = ContainerUtil.newArrayList();

  private ServerInfo myCurrentServerInfo;
  private WorkspaceInfo myCurrentWorkspaceInfo;

  @Override
  public void error(SAXParseException e) throws SAXException {
    throw e;
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    throw e;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if (SERVER_INFO.equals(qName)) {
      try {
        URI serverUri = new URI(attributes.getValue(URI_ATTR));
        myCurrentServerInfo =
          new ServerInfo(serverUri, attributes.getValue(GUID_ATTR), new TfsBeansHolder(serverUri));
      }
      catch (URISyntaxException e) {
        throw new SAXException(e);
      }
    }
    else if (WORKSPACE_INFO.equals(qName)) {
      String name = attributes.getValue(NAME_ATTR);
      String owner = attributes.getValue(OWNER_NAME_ATTR);
      String computer = attributes.getValue(COMPUTER_ATTR);
      String comment = attributes.getValue(COMMENT_ATTR);
      Calendar timestamp = ConverterUtil.convertToDateTime(attributes.getValue(TIMESTAMP_ATTR));
      String localWorkspace = attributes.getValue(IS_LOCAL_WORKSPACE_ATTR);
      String ownerDisplayName = attributes.getValue(OWNER_DISPLAY_NAME_ATTR);
      String securityToken = attributes.getValue(SECURITY_TOKEN_ATTR);
      int options = ConverterUtil.convertToInt(attributes.getValue(OPTIONS_ATTR));

      myCurrentWorkspaceInfo =
        new WorkspaceInfo(myCurrentServerInfo, name, owner, computer, comment, timestamp, Boolean.parseBoolean(localWorkspace),
                          ownerDisplayName, securityToken, options);
    }
    else if (MAPPED_PATH.equals(qName)) {
      myCurrentWorkspaceInfo
        .addWorkingFolderInfo(new WorkingFolderInfo(VcsUtil.getFilePath(attributes.getValue(PATH_ATTR), true)));
    }
    else if (OWNER_ALIAS.equals(qName)) {
      myCurrentWorkspaceInfo.addOwnerAlias(attributes.getValue(OWNER_ALIAS_ATTR));
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (SERVER_INFO.equals(qName)) {
      myServerInfos.add(myCurrentServerInfo);
      myCurrentServerInfo = null;
    }
    else if (WORKSPACE_INFO.equals(qName)) {
      myCurrentServerInfo.addWorkspaceInfo(myCurrentWorkspaceInfo);
      myCurrentWorkspaceInfo = null;
    }
  }

  @NotNull
  public List<ServerInfo> getServers() {
    return myServerInfos;
  }
}
