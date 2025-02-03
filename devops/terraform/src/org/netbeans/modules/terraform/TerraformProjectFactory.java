/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.terraform;

import java.io.IOException;
import java.util.Enumeration;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lkishalmi
 */
@ServiceProvider(service = ProjectFactory2.class, position = 8000)
public class TerraformProjectFactory implements ProjectFactory2 {

    public static final String TERRAFORM_PROJECT_TYPE = "terraform";

    private static final String TERRAFORM_ICON = "org/netbeans/modules/languages/hcl/resources/terraform.png";

    @Override
    public ProjectManager.Result isProject2(FileObject dir) {
        return isProject(dir) ? new ProjectManager.Result(dir.getNameExt(), "terraform", null) : null;
    }

    @Override
    public boolean isProject(FileObject dir) {
        Enumeration<? extends FileObject> it = dir.getChildren(false);
        while (it.hasMoreElements()) {
            FileObject fo = it.nextElement();
            if ("tf".equals(fo.getExt()) && fo.isData()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Project loadProject(FileObject dir, ProjectState state) throws IOException {
        return isProject(dir) ? new TerraformProject(dir, state) : null;
    }

    @Override
    public void saveProject(Project project) throws IOException, ClassCastException {
    }

}
