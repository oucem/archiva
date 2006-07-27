package org.apache.maven.repository.indexing.record;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.digest.Digester;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An index record type for the minimal index.
 *
 * @author Edwin Punzalan
 * @author Brett Porter
 * @plexus.component role="org.apache.maven.repository.indexing.record.RepositoryIndexRecordFactory" role-hint="minimal"
 */
public class MinimalArtifactIndexRecordFactory
    extends AbstractArtifactIndexRecordFactory
{
    /* List of types to index. */
    private static final Set INDEXED_TYPES = new HashSet( Arrays.asList( new String[]{"jar", "maven-plugin"} ) );

    public RepositoryIndexRecord createRecord( Artifact artifact )
    {
        MinimalArtifactIndexRecord record = null;

        File file = artifact.getFile();
        if ( file != null && INDEXED_TYPES.contains( artifact.getType() ) && file.exists() )
        {
            String md5 = readChecksum( file, Digester.MD5 );

            List files = null;
            try
            {
                files = readFilesInArchive( file );
            }
            catch ( IOException e )
            {
                getLogger().error( "Error reading artifact file, omitting from index: " + e.getMessage() );
            }

            if ( files != null )
            {
                record = new MinimalArtifactIndexRecord();
                record.setMd5Checksum( md5 );
                record.setFilename( artifact.getRepository().pathOf( artifact ) );
                record.setLastModified( file.lastModified() );
                record.setSize( file.length() );
                record.setClasses( getClassesFromFiles( files ) );
            }
        }
        return record;
    }

    private String getClassesFromFiles( List files )
    {
        StringBuffer classes = new StringBuffer();

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            if ( isClass( name ) )
            {
                classes.append( name.substring( 0, name.length() - 6 ).replace( '/', '.' ) ).append( "\n" );
            }
        }

        return classes.toString();
    }
}
