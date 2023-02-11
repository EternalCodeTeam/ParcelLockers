package com.eternalcode.parcellockers.updater;

import com.eternalcode.gitcheck.GitCheck;
import com.eternalcode.gitcheck.GitCheckResult;
import com.eternalcode.gitcheck.git.GitRelease;
import com.eternalcode.gitcheck.git.GitRepository;
import com.eternalcode.gitcheck.git.GitTag;

import java.util.logging.Logger;

public class Updater {

    private final Logger logger;

    public Updater(Logger logger) {
        this.logger = logger;
    }

    public void start() {
        GitCheck gitCheck = new GitCheck();
        GitRepository repository = GitRepository.of("EternalCodeTeam", "ParcelLockers");

        GitCheckResult result = gitCheck.checkRelease(repository, GitTag.of("v1.0.0"));

        if (!result.isUpToDate()) {
            GitRelease release = result.getLatestRelease();
            GitTag tag = release.getTag();

            this.logger.info("A new version is available: " + tag.getTag());
            this.logger.info("See release page: " + release.getPageUrl());
            this.logger.info("Release date: " + release.getPublishedAt());
        }

    }
}
