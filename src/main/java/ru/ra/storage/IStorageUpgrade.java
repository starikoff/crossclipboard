package ru.ra.storage;

public interface IStorageUpgrade<S extends IUpgradableStorage<S>> {
    int getVersion();

    void upgrade(S storage) throws UpgradeException;
}
