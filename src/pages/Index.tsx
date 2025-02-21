
import { CheckCircle } from "lucide-react";

const Index = () => {
  return (
    <table width="100%" cellPadding="0" cellSpacing="0" style={{ backgroundColor: '#F9FAFB', minHeight: '100vh' }}>
      <tr>
        <td align="center" style={{ padding: '16px' }}>
          <table width="600" cellPadding="0" cellSpacing="0" style={{ backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
            {/* Header */}
            <tr>
              <td align="center" style={{ padding: '32px 32px 24px 32px' }}>
                <table cellPadding="0" cellSpacing="0">
                  <tr>
                    <td align="center">
                      <div style={{ width: '64px', height: '64px', backgroundColor: '#D1FAE5', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <CheckCircle size={32} color="#059669" />
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td align="center" style={{ paddingTop: '16px' }}>
                      <h1 style={{ margin: '0', fontSize: '24px', fontWeight: '600', color: '#111827' }}>Pagamento Aprovado</h1>
                      <p style={{ margin: '8px 0 0', color: '#4B5563' }}>Seu pagamento foi processado com sucesso</p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>

            {/* Separator */}
            <tr>
              <td style={{ borderTop: '1px solid #E5E7EB' }}></td>
            </tr>

            {/* Order Status */}
            <tr>
              <td style={{ padding: '24px 32px' }}>
                <table width="100%" cellPadding="0" cellSpacing="0">
                  {/* Order Number and Track Button */}
                  <tr>
                    <td>
                      <table width="100%" cellPadding="16" cellSpacing="0" style={{ backgroundColor: '#FEF7CD', borderRadius: '8px' }}>
                        <tr>
                          <td>
                            <table cellPadding="0" cellSpacing="0">
                              <tr>
                                <td style={{ fontSize: '18px', fontWeight: '500', color: '#111827' }}>Status do Pedido</td>
                                <td style={{ paddingLeft: '8px', fontSize: '14px', color: '#6B7280' }}>#123456789</td>
                              </tr>
                            </table>
                          </td>
                          <td align="right">
                            <button style={{ 
                              padding: '8px 16px', 
                              border: '1px solid #059669', 
                              borderRadius: '6px', 
                              backgroundColor: 'transparent',
                              color: '#059669',
                              cursor: 'pointer'
                            }}>
                              Rastrear Pedido
                            </button>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  {/* Status Timeline */}
                  <tr>
                    <td style={{ paddingTop: '16px' }}>
                      <table width="100%" cellPadding="0" cellSpacing="0">
                        {/* Order Placed */}
                        <tr>
                          <td width="48" style={{ paddingTop: '16px', paddingBottom: '16px' }}>
                            <div style={{ width: '44px', height: '44px', backgroundColor: '#059669', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              <CheckCircle size={20} color="white" />
                            </div>
                          </td>
                          <td style={{ paddingLeft: '16px' }}>
                            <h3 style={{ margin: '0', fontWeight: '500', color: '#111827' }}>Pedido Realizado</h3>
                            <p style={{ margin: '4px 0 0', fontSize: '14px', color: '#6B7280' }}>21/03/2024 às 10:30</p>
                          </td>
                        </tr>
                        {/* Payment Approved */}
                        <tr>
                          <td width="48" style={{ paddingTop: '16px', paddingBottom: '16px' }}>
                            <div style={{ width: '44px', height: '44px', backgroundColor: '#059669', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              <CheckCircle size={20} color="white" />
                            </div>
                          </td>
                          <td style={{ paddingLeft: '16px' }}>
                            <h3 style={{ margin: '0', fontWeight: '500', color: '#111827' }}>Pagamento Aprovado</h3>
                            <p style={{ margin: '4px 0 0', fontSize: '14px', color: '#6B7280' }}>21/03/2024 às 10:35</p>
                          </td>
                        </tr>
                        {/* Order Shipped */}
                        <tr>
                          <td width="48" style={{ paddingTop: '16px', paddingBottom: '16px' }}>
                            <div style={{ width: '44px', height: '44px', backgroundColor: '#E5E7EB', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              <CheckCircle size={20} color="#9CA3AF" />
                            </div>
                          </td>
                          <td style={{ paddingLeft: '16px' }}>
                            <h3 style={{ margin: '0', fontWeight: '500', color: '#9CA3AF' }}>Pedido Enviado</h3>
                            <p style={{ margin: '4px 0 0', fontSize: '14px', color: '#9CA3AF' }}>Aguardando</p>
                          </td>
                        </tr>
                        {/* Order Delivered */}
                        <tr>
                          <td width="48" style={{ paddingTop: '16px', paddingBottom: '16px' }}>
                            <div style={{ width: '44px', height: '44px', backgroundColor: '#E5E7EB', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              <CheckCircle size={20} color="#9CA3AF" />
                            </div>
                          </td>
                          <td style={{ paddingLeft: '16px' }}>
                            <h3 style={{ margin: '0', fontWeight: '500', color: '#9CA3AF' }}>Pedido Entregue</h3>
                            <p style={{ margin: '4px 0 0', fontSize: '14px', color: '#9CA3AF' }}>Aguardando</p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>

            {/* Separator */}
            <tr>
              <td style={{ borderTop: '1px solid #E5E7EB' }}></td>
            </tr>

            {/* Transaction Details */}
            <tr>
              <td style={{ padding: '24px 32px' }}>
                <h2 style={{ margin: '0 0 16px', fontSize: '18px', fontWeight: '500', color: '#111827' }}>Detalhes da Transação</h2>
                <table width="100%" cellPadding="0" cellSpacing="0">
                  <tr>
                    <td width="50%" style={{ paddingTop: '8px', paddingBottom: '8px' }}>
                      <p style={{ margin: '0', color: '#6B7280' }}>ID do Pedido</p>
                      <p style={{ margin: '4px 0 0', fontWeight: '500', color: '#111827' }}>#123456789</p>
                    </td>
                    <td width="50%" style={{ paddingTop: '8px', paddingBottom: '8px' }}>
                      <p style={{ margin: '0', color: '#6B7280' }}>Data</p>
                      <p style={{ margin: '4px 0 0', fontWeight: '500', color: '#111827' }}>21/03/2024</p>
                    </td>
                  </tr>
                  <tr>
                    <td width="50%" style={{ paddingTop: '8px', paddingBottom: '8px' }}>
                      <p style={{ margin: '0', color: '#6B7280' }}>Método</p>
                      <p style={{ margin: '4px 0 0', fontWeight: '500', color: '#111827' }}>Cartão de Crédito</p>
                    </td>
                    <td width="50%" style={{ paddingTop: '8px', paddingBottom: '8px' }}>
                      <p style={{ margin: '0', color: '#6B7280' }}>Valor</p>
                      <p style={{ margin: '4px 0 0', fontWeight: '500', color: '#111827' }}>R$ 299,90</p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>

            {/* Separator */}
            <tr>
              <td style={{ borderTop: '1px solid #E5E7EB' }}></td>
            </tr>

            {/* Actions */}
            <tr>
              <td style={{ padding: '24px 32px' }}>
                <button style={{ 
                  width: '100%',
                  padding: '12px',
                  backgroundColor: '#059669',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}>
                  Ver Detalhes do Pedido
                </button>
              </td>
            </tr>

            {/* Footer */}
            <tr>
              <td align="center" style={{ padding: '0 32px 32px' }}>
                <p style={{ margin: '0', fontSize: '14px', color: '#6B7280' }}>Precisa de ajuda? Entre em contato com nosso suporte</p>
                <p style={{ margin: '8px 0 0', fontSize: '14px', color: '#6B7280' }}>suporte@empresa.com.br | (11) 99999-9999</p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  );
};

export default Index;
