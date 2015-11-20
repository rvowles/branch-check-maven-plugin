package com.bluetrainsoftware.maven.branchchange

import groovy.transform.CompileStatic
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser

/**
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
@CompileStatic
@Mojo(
	name = 'change-track',
	requiresProject = true,
	requiresDependencyResolution = ResolutionScope.NONE,
	defaultPhase = LifecyclePhase.PREPARE_PACKAGE
)
class BranchChangeMojo extends AbstractMojo {
	@Parameter(property = "trackedBranch")
	String trackedBranch

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {

		if (!trackedBranch) {
			throw new MojoExecutionException("No trackedBranch configured.")
		}

		Repository repository = buildRepository()

		Git git = new Git(repository)
		Iterable<RevCommit> logs = git.log().call()
		Map<ObjectId, RevCommit> currentBranchCommits = new HashMap<>()

		for(RevCommit log : logs) {
			currentBranchCommits[log.id] = log
		}

		Ref ref = repository.getRef(trackedBranch)

		if (ref) {
			int count = 0

			for(RevCommit log : git.log().add(ref.objectId).call()) {
				if (!currentBranchCommits[log.id]) {
					count ++
					getLog().error("Extra commit in tracked branch ${log.id.name()}:  ${log.shortMessage}")
				}
			}

			if (count > 0) {
				throw new MojoExecutionException("Tracked branch has unmerged changes.")
			}
		}
	}

	/**
	 * @return current repository build
	 */
	protected static Repository buildRepository() {
		return new RepositoryBuilder().findGitDir().build()
	}

}
