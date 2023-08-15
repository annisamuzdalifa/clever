import {
  SimpleGrid,
  Box,
  TableContainer,
  Table,
  Thead,
  Tbody,
  Th,
  Td,
  Tr

} from "@chakra-ui/react";
import Card from "components/card/Card.js";
import React from "react";
import { useQueries } from "@tanstack/react-query";

import axios from 'axios';

export default function History() {
  const hostAddress = localStorage.getItem('host');
  const [transactionHistory] = useQueries({
    queries: [
      {
        refetchInterval: 4000,
        queryKey: ['transactionHistoryKey'],
        queryFn: () =>
          axios
            //.get(`http://localhost:8080/v2/transaction-history`)
            .get(`${hostAddress}/transaction-history`)
            .then((res) => res.data),
      },
    ],
  });
  const getTableHistoryComponents = (transactionHistory) => {
    if (transactionHistory.isLoading) return 'Loading data...';
    if (transactionHistory.error) { return 'service temporarily not available'; }
    return <TableContainer>
      <Table size='sm'>
        <Thead>
          <Tr>
            <Th>Date</Th>
            <Th>Sender</Th>
            <Th>Curr Origin</Th>
            <Th>Curr Dest</Th>
            <Th>Amt Recv</Th>
            <Th>rate</Th>
            <Th>fee %</Th>
            <Th>fee amt</Th>
          </Tr>
        </Thead>
        <Tbody>
          {transactionHistory.data.map((i) =>
            <Tr>
            <Td>{i.date}</Td>
            <Td>{i.sender}</Td>
            <Td>{i.currencyOrigin}</Td>
            <Td>{i.currencyDest}</Td>
            <Td>{i.amountReceived}</Td>
            <Td>{i.rate}</Td>
            <Td>{i.feePercentage}</Td>
            <Td>{i.feeAmount}</Td>
            </Tr>
          )}
        </Tbody>
      </Table>
    </TableContainer>
  }
  return (
    <Box pt={{ base: "130px", md: "80px", xl: "80px" }}>
            <SimpleGrid>
        <Card
      direction='column'
      w='100%'
      px='0px'
      overflowX={{ sm: "scroll", lg: "hidden" }}>
      <TableContainer>
      {getTableHistoryComponents(transactionHistory)}
      </TableContainer>
      </Card>
      </SimpleGrid>
    </Box>
  );
}
