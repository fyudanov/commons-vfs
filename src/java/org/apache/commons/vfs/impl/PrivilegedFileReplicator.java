/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.commons.vfs.impl;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.FileReplicator;
import org.apache.commons.vfs.provider.FileSystemProviderContext;
import org.apache.commons.vfs.provider.VfsComponent;
import org.apache.commons.logging.Log;

/**
 * A file replicator that wraps another file replicator, performing
 * the replication as a privileged action.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 1.5 $ $Date: 2002/10/21 01:40:38 $
 */
public class PrivilegedFileReplicator
    implements FileReplicator, VfsComponent
{
    private final FileReplicator replicator;
    private final VfsComponent replicatorComponent;

    public PrivilegedFileReplicator( FileReplicator replicator )
    {
        this.replicator = replicator;
        if ( replicator instanceof VfsComponent )
        {
            replicatorComponent = (VfsComponent)replicator;
        }
        else
        {
            replicatorComponent = null;
        }
    }

    /**
     * Sets the Logger to use for the component.
     */
    public void setLogger( final Log logger )
    {
        if ( replicatorComponent != null  )
        {
            replicatorComponent.setLogger( logger );
        }
    }

    /**
     * Sets the context for the replicator.
     */
    public void setContext( final FileSystemProviderContext context )
    {
        if ( replicatorComponent != null )
        {
            replicatorComponent.setContext( context );
        }
    }

    /**
     * Initialises the component.
     */
    public void init() throws FileSystemException
    {
        if ( replicatorComponent != null )
        {
            try
            {
                AccessController.doPrivileged( new InitAction() );
            }
            catch ( final PrivilegedActionException e )
            {
                throw new FileSystemException( "vfs.impl/init-replicator.error", null, e );
            }
        }
    }

    /**
     * Closes the replicator.
     */
    public void close()
    {
        if ( replicatorComponent != null )
        {
            AccessController.doPrivileged( new CloseAction() );
        }
    }

    /**
     * Creates a local copy of the file, and all its descendents.
     */
    public File replicateFile( FileObject srcFile, FileSelector selector )
        throws FileSystemException
    {
        try
        {
            final ReplicateAction action = new ReplicateAction( srcFile, selector );
            return (File)AccessController.doPrivileged( action );
        }
        catch ( final PrivilegedActionException e )
        {
            throw new FileSystemException( "vfs.impl/replicate-file.error", new Object[]{srcFile.getName()}, e );
        }
    }

    /** An action that initialises the wrapped replicator. */
    private class InitAction implements PrivilegedExceptionAction
    {
        /**
         * Performs the action.
         */
        public Object run() throws Exception
        {
            replicatorComponent.init();
            return null;
        }
    }

    /** An action that replicates a file using the wrapped replicator. */
    private class ReplicateAction implements PrivilegedExceptionAction
    {
        private final FileObject srcFile;
        private final FileSelector selector;

        public ReplicateAction( final FileObject srcFile,
                                final FileSelector selector )
        {
            this.srcFile = srcFile;
            this.selector = selector;
        }

        /** Performs the action. */
        public Object run() throws Exception
        {
            // TODO - Do not pass the selector through.  It is untrusted
            // TODO - Need to determine which files can be read
            return replicator.replicateFile( srcFile, selector );
        }
    }

    /** An action that closes the wrapped replicator. */
    private class CloseAction implements PrivilegedAction
    {
        /** Performs the action. */
        public Object run()
        {
            replicatorComponent.close();
            return null;
        }
    }
}
