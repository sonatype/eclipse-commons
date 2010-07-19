package org.maven.ide.eclipse.authentication;


public interface IAuthRealm
{
    public String getId();

    public String getName();

    public void setName( String name );

    public String getDescription();

    public void setDescription( String description );

    public IAuthData getAuthData();

    public ISSLAuthData getSSLAuthData();

    public void setSSLAuthData( ISSLAuthData authData );

    public boolean isAnonymous();

    public void setUsername( String username );

    public void setPassword( String password );
}
