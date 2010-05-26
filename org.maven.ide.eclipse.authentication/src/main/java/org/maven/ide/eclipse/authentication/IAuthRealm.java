package org.maven.ide.eclipse.authentication;

public interface IAuthRealm
{
    public String getId();

    public IAuthData getAuthData();
    
    public boolean isAnonymous();

    public void setUsername( String username );

    public void setPassword( String password );
}
