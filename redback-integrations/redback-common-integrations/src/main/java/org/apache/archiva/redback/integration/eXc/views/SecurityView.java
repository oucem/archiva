package org.apache.archiva.redback.integration.eXc.views;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.extremecomponents.table.core.TableModel;
import org.extremecomponents.table.view.AbstractHtmlView;
import org.extremecomponents.util.HtmlBuilder;

/**
 * SecurityView
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class SecurityView
    extends AbstractHtmlView
{
    protected void beforeBodyInternal( TableModel model )
    {
        getTableBuilder().tableStart();

        getTableBuilder().theadStart();

        getTableBuilder().titleRowSpanColumns();
        
        navigationToolbar( getHtmlBuilder(), getTableModel() );
        
        getTableBuilder().headerRow();

        getTableBuilder().theadEnd();

        getTableBuilder().filterRow();

        getTableBuilder().tbodyStart();
    }

    protected void afterBodyInternal( TableModel model )
    {
        getCalcBuilder().defaultCalcLayout();

        getTableBuilder().tbodyEnd();

        getTableBuilder().tableEnd();
    }

    protected void navigationToolbar( HtmlBuilder html, TableModel model )
    {
        new SecurityToolbar( html, model ).layout();
    }
}
