package com.fileflow.repository;

import com.fileflow.model.File;
import com.fileflow.model.FileTag;
import com.fileflow.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileTagRepository extends JpaRepository<FileTag, Long> {
    List<FileTag> findByFile(File file);

    List<FileTag> findByTag(Tag tag);

    Optional<FileTag> findByFileAndTag(File file, Tag tag);

    void deleteByFileAndTag(File file, Tag tag);
}