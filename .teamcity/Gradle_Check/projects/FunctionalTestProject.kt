package projects

import configurations.FunctionalTest
import configurations.shouldBeSkipped
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import model.CIBuildModel
import model.Stage
import model.TestCoverage

class FunctionalTestProject(model: CIBuildModel, testConfig: TestCoverage, stage: Stage) : Project({
    this.uuid = testConfig.asId(model)
    this.id = uuid
    this.name = testConfig.asName()

    model.subProjects.forEach { subProject ->
        if (shouldBeSkipped(subProject, testConfig)) {
            return@forEach
        }
        if (subProject.containsSlowTests && stage.omitsSlowProjects) {
            addMissingTestCoverage(testConfig)
            return@forEach
        }

        if (subProject.unitTests && testConfig.testType.unitTests) {
            buildType(FunctionalTest(model, testConfig, subProject.name, stage))
        } else if (subProject.functionalTests && testConfig.testType.functionalTests) {
            buildType(FunctionalTest(model, testConfig, subProject.name, stage))
        } else if (subProject.crossVersionTests && testConfig.testType.crossVersionTests) {
            buildType(FunctionalTest(model, testConfig, subProject.name, stage))
        }
    }
}){
    companion object {
        val missingTestCoverage = mutableListOf<TestCoverage>()

        private fun addMissingTestCoverage(coverage: TestCoverage) {
            this.missingTestCoverage.add(coverage)
        }
    }
}
