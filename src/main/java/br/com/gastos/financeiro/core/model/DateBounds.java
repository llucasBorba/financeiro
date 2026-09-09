package br.com.gastos.financeiro.core.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Faixa de datas que o domínio aceita.
 *
 * <p>Existe porque {@link LocalDate} vai do ano -999999999 ao 999999999, e nada entre o
 * JSON e o banco estreitava isso. O Jackson aceita {@code "+999999999-12-31"} (a ISO-8601
 * permite anos com mais de 4 dígitos desde que venham com sinal explícito), e o valor
 * atravessava a aplicação inteira até o driver JDBC.
 *
 * <p>E o driver não reclamava: o pgjdbc traduz {@link LocalDate#MAX} para o valor especial
 * {@code infinity} do Postgres. O resultado era 201 Created gravando uma linha que nenhum
 * filtro de mês jamais encontra — a despesa existia, mas sumia de todo relatório. Um erro
 * silencioso, que é pior que um 500: ninguém percebe.
 *
 * <p>O limite aqui é uma decisão de negócio, não uma limitação do banco: um app de finanças
 * pessoais não tem vencimento no ano 3000. As restrições CHECK no schema são a segunda
 * barreira — mesma relação entre o domínio e o banco que já usamos com o Flyway, onde as
 * migrações mandam e o {@code ddl-auto=validate} audita.
 */
public final class DateBounds {

    /** Antes disto não é finanças pessoais, é erro de digitação. */
    public static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);

    /**
     * Teto do domínio. Coincide com o limite que {@code RecurringExpense} já usava para
     * barrar gerações absurdas — é o mesmo horizonte, declarado num lugar só.
     */
    public static final LocalDate LATEST = LocalDate.of(2200, 12, 31);

    public static final YearMonth EARLIEST_MONTH = YearMonth.from(EARLIEST);
    public static final YearMonth LATEST_MONTH = YearMonth.from(LATEST);

    private DateBounds() {
    }

    /**
     * @return a própria data, para poder ser usada dentro de uma atribuição.
     * @throws IllegalArgumentException se estiver fora da faixa (vira 400 no handler global).
     */
    public static LocalDate require(LocalDate date, String campo) {
        if (date == null) {
            return null;
        }
        if (date.isBefore(EARLIEST) || date.isAfter(LATEST)) {
            throw new IllegalArgumentException(fora(campo));
        }
        return date;
    }

    public static LocalDateTime require(LocalDateTime instante, String campo) {
        if (instante == null) {
            return null;
        }
        require(instante.toLocalDate(), campo);
        return instante;
    }

    public static YearMonth require(YearMonth month, String campo) {
        if (month == null) {
            return null;
        }
        if (month.isBefore(EARLIEST_MONTH) || month.isAfter(LATEST_MONTH)) {
            throw new IllegalArgumentException(fora(campo));
        }
        return month;
    }

    private static String fora(String campo) {
        return campo + " deve estar entre " + EARLIEST + " e " + LATEST + ".";
    }
}
