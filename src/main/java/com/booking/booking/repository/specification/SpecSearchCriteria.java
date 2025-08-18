package com.booking.booking.repository.specification;

import static com.booking.booking.repository.specification.SearchOperation.CONTAINS;
import static com.booking.booking.repository.specification.SearchOperation.ENDS_WITH;
import static com.booking.booking.repository.specification.SearchOperation.EQUALITY;
import static com.booking.booking.repository.specification.SearchOperation.OR_PREDICATE_FLAG;
import static com.booking.booking.repository.specification.SearchOperation.STARTS_WITH;
import static com.booking.booking.repository.specification.SearchOperation.ZERO_OR_MORE_REGEX;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SpecSearchCriteria {

  private String key;
  private SearchOperation operation;
  private Object value;
  private boolean orPredicate;


  public SpecSearchCriteria(final String key, final SearchOperation operation, final Object value) {
    super();
    this.key = key;
    this.operation = operation;
    this.value = value;
  }

  public SpecSearchCriteria(final String orPredicate, final String key,
      final SearchOperation operation, final Object value) {
    super();
    this.orPredicate = orPredicate != null && orPredicate.equals(OR_PREDICATE_FLAG);
    this.key = key;
    this.operation = operation;
    this.value = value;
  }

  public SpecSearchCriteria(String key, String operation, String prefix, String value,
      String suffix) {
    SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation.charAt(0));
    if (searchOperation != null) {
      if (searchOperation == EQUALITY) { // the operation may be complex operation
        final boolean startWithAsterisk = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
        final boolean endWithAsterisk = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);

        if (startWithAsterisk && endWithAsterisk) {
          searchOperation = CONTAINS;
        } else if (startWithAsterisk) {
          searchOperation = ENDS_WITH;
        } else if (endWithAsterisk) {
          searchOperation = STARTS_WITH;
        }
      }
    }
    this.key = key;
    this.operation = searchOperation;
    this.value = value;
  }

}
