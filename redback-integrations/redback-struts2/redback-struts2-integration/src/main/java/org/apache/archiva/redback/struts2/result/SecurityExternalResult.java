package org.apache.archiva.redback.struts2.result;

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
import com.opensymphony.xwork2.ActionInvocation;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;


/**
 * SecurityExternalResult
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@SuppressWarnings( "serial" )
@Controller( "securityExternalResult" )
@Scope( "prototype" )
public class SecurityExternalResult
    extends AbstractBackTrackingResult
{
    /**
     *
     */
    private String externalActionName = "redbackRedirect";

    private String externalResult;

    @Override
    public void execute( ActionInvocation invocation )
        throws Exception
    {
        // the login redirection is not captured by the http request
        // tracker, so we backtrack to the current request
        if ( !setupBackTrackCurrent( invocation ) )
        {
            setNamespace( "/" );
            setActionName( externalActionName );
        }

        super.execute( invocation );
    }

    public String getExternalResult()
    {
        return externalResult;
    }

    public void setExternalResult( String externalResult )
    {
        this.externalResult = externalResult;
    }

}
