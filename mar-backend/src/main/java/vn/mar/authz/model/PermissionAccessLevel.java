package vn.mar.authz.model;

public enum PermissionAccessLevel {
    NONE,
    VIEW,
    CREATE,
    UPDATE,
    MANAGE,
    FULL;

    public boolean grantsAccess() {
        return this != NONE;
    }
}
