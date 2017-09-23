package ru.ra.storage;

import java.util.List;

import javax.annotation.Nonnull;

import ru.ra.AuthInfo;
import ru.ra.errors.ContentReadException;
import ru.ra.errors.ContentWriteException;
import ru.ra.storage.INote.INoteCoord;

public interface IContentStorage {

    @Nonnull
    List<INote> get(AuthInfo auth) throws ContentReadException;

    void remove(INoteCoord noteCoord) throws ContentWriteException;

    INote addNote(AuthInfo auth, String content) throws ContentWriteException;

    void makeLink(String id, String title, String favicon)
            throws ContentWriteException;

    int getContentSize(AuthInfo auth) throws ContentReadException;

    int getSizeLimit();

    INote get(INoteCoord noteCoord) throws ContentReadException;
}
