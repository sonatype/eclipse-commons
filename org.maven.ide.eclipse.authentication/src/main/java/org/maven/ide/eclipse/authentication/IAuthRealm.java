package org.maven.ide.eclipse.authentication;


public interface IAuthRealm
{
    public String getId();

    public String getName();

    public void setName( String name );

    public String getDescription();

    public void setDescription( String description );

    IAuthData getAuthData();

    boolean setAuthData( IAuthData authData );

    public AuthenticationType getAuthenticationType();

    public void setAuthenticationType( AuthenticationType authenticationType );
}
