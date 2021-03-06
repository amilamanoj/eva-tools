/*
 * Copyright 2017 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.dbsnpimporter.jobs.steps.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import uk.ac.ebi.eva.dbsnpimporter.models.SubSnpCoreFields;

/**
 * Filters out those variants without coordinates in a chromosome *or* contig.
 */
public class MissingCoordinatesFilterProcessor implements ItemProcessor<SubSnpCoreFields, SubSnpCoreFields> {

    private static final Logger logger = LoggerFactory.getLogger(MissingCoordinatesFilterProcessor.class);

    @Override
    public SubSnpCoreFields process(SubSnpCoreFields subSnpCoreFields) {
        if (subSnpCoreFields.getVariantCoordinates() == null) {
            logger.debug("Variant filtered out because it does not have a genomic location: {}", subSnpCoreFields);
            return null;
        }

        return subSnpCoreFields;
    }

}
