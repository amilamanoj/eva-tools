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
package uk.ac.ebi.eva.dbsnpimporter.io.readers;

import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;

import uk.ac.ebi.eva.dbsnpimporter.models.SubSnpCoreFields;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.ALLELES;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.ALTERNATE;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CHROMOSOME_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CHROMOSOME_END_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CHROMOSOME_START_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CONTIG_END_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CONTIG_NAME_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CONTIG_ORIENTATION_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.CONTIG_START_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_C_ORIENTATION;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_C_START;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_C_STOP;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_C_STRING;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_T_ORIENTATION;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_T_START;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_T_STOP;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.HGVS_T_STRING;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.REFERENCE_C;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.REFERENCE_T;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.REFSNP_ID_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.SNP_ORIENTATION_COLUMN;
import static uk.ac.ebi.eva.dbsnpimporter.io.readers.SubSnpCoreFieldsRowMapper.SUBSNP_ID_COLUMN;

/**
    SELECT distinct
        loc.snp_id AS rs_id,
        sub.subsnp_id AS ss_id,
        hgvs.hgvs_c as hgvs_c_string,
        hgvs.start_c+1 as hgvs_c_start,
        hgvs.stop_c+1 as hgvs_c_stop,
        hgvs.ref_allele_c as reference_c,
        hgvs.hgvs_t as hgvs_t_string,
        hgvs.start_t+1 as hgvs_t_start,
        hgvs.stop_t+1 as hgvs_t_stop,
        hgvs.ref_allele_t as reference_t,
        hgvs.var_allele as alternate,
        obsvariation.pattern AS alleles
        ctg.contig_acc AS contig_accession,
        ctg.contig_gi AS contig_id,
        loc.lc_ngbr+2 AS contig_start,
        loc.rc_ngbr AS contig_end,
        ctg.contig_chr AS chromosome,
        loc.phys_pos_from+1 AS chromosome_start,
        loc.phys_pos_from+1 + loc.asn_to - loc.asn_from AS chromosome_end,
        CASE
            WHEN hgvs.orient_c = 2 THEN -1 ELSE 1
        END AS hgvs_orientation,
        CASE
            WHEN loc.orientation = 1 THEN -1 ELSE 1
        END AS snp_orientation,
        CASE
            WHEN ctg.orient = 1 THEN -1 ELSE 1
        END AS contig_orientation
    FROM
        b150_snpcontigloc loc JOIN
        b150_contiginfo ctg ON ctg.ctg_id = loc.ctg_id JOIN
        snpsubsnplink link ON loc.snp_id = link.snp_id JOIN
        subsnp sub ON link.subsnp_id = sub.subsnp_id JOIN
        b150_snphgvslink hgvs ON hgvs.snp_link = loc.snp_id
    WHERE
        ctg.group_term IN($assemblyTypes)
        AND ctg.group_label LIKE '$assembly'
    ORDER BY ss_id ASC;
 */
public class SubSnpCoreFieldsReader extends JdbcPagingItemReader<SubSnpCoreFields> {

    public SubSnpCoreFieldsReader(String assembly, List<String> assemblyTypes, DataSource dataSource, int pageSize)
            throws Exception {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }

        setDataSource(dataSource);
        setQueryProvider(createQueryProvider(dataSource));
        setRowMapper(new SubSnpCoreFieldsRowMapper());
        setPageSize(pageSize);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("assemblyType", String.join(", ", assemblyTypes));
        parameterValues.put("assembly", assembly);
        setParameterValues(parameterValues);
    }

    private PagingQueryProvider createQueryProvider(DataSource dataSource) throws Exception {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setSelectClause(
                "SELECT distinct " +
                        "sub.subsnp_id AS " + SUBSNP_ID_COLUMN +
                        ",loc.snp_id AS " + REFSNP_ID_COLUMN +
                        ",hgvs.hgvs_c AS " + HGVS_C_STRING +
                        ",hgvs.start_c+1 AS " + HGVS_C_START +
                        ",hgvs.stop_c+1 AS " + HGVS_C_STOP +
                        ",hgvs.ref_allele_c AS " + REFERENCE_C +
                        ",hgvs.hgvs_t AS " + HGVS_T_STRING +
                        ",hgvs.start_t+1 AS " + HGVS_T_START +
                        ",hgvs.stop_t+1 AS " + HGVS_T_STOP +
                        ",hgvs.ref_allele_t AS " + REFERENCE_T +
                        ",hgvs.var_allele AS " + ALTERNATE +
                        ",obsvariation.pattern AS " + ALLELES +
                        ",ctg.contig_name AS " + CONTIG_NAME_COLUMN +
                        ",loc.asn_from +1 AS " + CONTIG_START_COLUMN +
                        ",loc.asn_to +1 AS " + CONTIG_END_COLUMN +
                        ",ctg.contig_chr AS " + CHROMOSOME_COLUMN +
                        ",loc.phys_pos_from + 1 AS " + CHROMOSOME_START_COLUMN +
                        ",loc.phys_pos_from + 1 + loc.asn_to - loc.asn_from AS " + CHROMOSOME_END_COLUMN +
                        ",CASE " +
                        "   WHEN hgvs.orient_c = 2 THEN -1 ELSE 1 " +
                        "END AS " + HGVS_C_ORIENTATION +
                        ",CASE " +
                        "   WHEN hgvs.orient_t = 2 THEN -1 ELSE 1 " +
                        "END AS " + HGVS_T_ORIENTATION +
                        ",CASE " +
                        "   WHEN loc.orientation = 1 THEN -1 ELSE 1 " +
                        "END AS " + SNP_ORIENTATION_COLUMN +
                        ",CASE " +
                        "   WHEN ctg.orient = 1 THEN -1 ELSE 1 " +
                        "END AS " + CONTIG_ORIENTATION_COLUMN
        );
        factoryBean.setFromClause(
                "FROM " +
                        "b150_snpcontigloc loc JOIN " +
                        "b150_contiginfo ctg ON ctg.ctg_id = loc.ctg_id JOIN " +
                        "snpsubsnplink link ON loc.snp_id = link.snp_id JOIN " +
                        "subsnp sub ON link.subsnp_id = sub.subsnp_id JOIN " +
                        "b150_snphgvslink hgvs ON hgvs.snp_link = loc.snp_id JOIN " +
                        "obsvariation on obsvariation.var_id = sub.variation_id"
        );
        factoryBean.setWhereClause(
                "WHERE " +
                        "ctg.group_term IN (:assemblyType) AND " +
                        "ctg.group_label LIKE :assembly"
        );
        factoryBean.setSortKey(SUBSNP_ID_COLUMN);

        return factoryBean.getObject();
    }
}