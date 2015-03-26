/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class PersistCoverageStepTest extends BaseStepTest {

  private static final Integer FILE_REF = 3;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  PersistCoverageStep step;

  @Before
  public void setup() throws Exception {
    reportDir = temp.newFolder();
    step = new PersistCoverageStep();
  }

  @Override
  protected ComputationStep step() throws IOException {
    return step;
  }

  @Test
  public void compute_nothing() throws Exception {
    initReport();

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(step.getFileSourceData()).isNull();
  }

  @Test
  public void compute_coverage() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeFileCoverage(BatchReport.Coverage.newBuilder()
      .setFileRef(FILE_REF)
      .addAllConditionsByLine(Arrays.asList(1, 5))
      .addAllUtHitsByLine(Arrays.asList(true, false))
      .addAllItHitsByLine(Arrays.asList(false, false))
      .addAllUtCoveredConditionsByLine(Arrays.asList(1, 4))
      .addAllItCoveredConditionsByLine(Arrays.asList(1, 5))
      .addAllOverallCoveredConditionsByLine(Arrays.asList(1, 5))
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    FileSourceDb.Data data = step.getFileSourceData();
    assertThat(data.getLines(0).getUtLineHits()).isEqualTo(10);
    assertThat(data.getLines(0).getItLineHits()).isEqualTo(11);

    assertThat(data.getLines(1).hasUtLineHits()).isFalse();
    assertThat(data.getLines(1).getItLineHits()).isEqualTo(4);

    assertThat(data.getLines(2).getUtLineHits()).isEqualTo(4);
  }

  private BatchReportWriter initReport() {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("PROJECT_A")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("BCDE")
      .addChildRef(FILE_REF)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(FILE_REF)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    return writer;
  }

}
