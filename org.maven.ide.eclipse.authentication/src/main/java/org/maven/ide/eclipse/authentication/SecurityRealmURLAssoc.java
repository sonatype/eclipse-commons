package org.maven.ide.eclipse.authentication;

public class SecurityRealmURLAssoc
    implements ISecurityRealmURLAssoc
{
    private String id;

    private String realmId;

    private String url;

    private AnonymousAccessType anonymousAccess = AnonymousAccessType.NOT_ALLOWED;

    public SecurityRealmURLAssoc( String id, String url, String realmId, AnonymousAccessType anonymousAccess ) {
        this.id = id;
        this.realmId = realmId;
        this.url = url;
        this.anonymousAccess = anonymousAccess;
    }

    public String getId()
    {
        return id;
    }

    public String getRealmId()
    {
        return realmId;
    }
    public void setRealmId( String realmId )
    {
        this.realmId = realmId;
    }
    public String getUrl()
    {
        return url;
    }
    public void setUrl( String url )
    {
        this.url = url;
    }
    public AnonymousAccessType getAnonymousAccess()
    {
        return anonymousAccess;
    }
    public void setAnonymousAccess( AnonymousAccessType anonymousAccess )
    {
        this.anonymousAccess = anonymousAccess;
    }
}
