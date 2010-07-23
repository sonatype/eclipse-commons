package org.maven.ide.eclipse.authentication;


public interface ISecurityRealmURLAssoc
{
    public String getId();

    public String getRealmId();

    public String getUrl();

    public void setRealmId( String realmId );

    public void setUrl( String url );

    public void setAnonymousAccess( AnonymousAccessType anonymousAccess );

    public AnonymousAccessType getAnonymousAccess();
}
